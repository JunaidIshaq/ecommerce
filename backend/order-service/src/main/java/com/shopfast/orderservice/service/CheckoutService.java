package com.shopfast.orderservice.service;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.common.enums.NotificationType;
import com.shopfast.common.events.CartItemDto;
import com.shopfast.common.events.CouponLineItemDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.CouponValidateResponseDto;
import com.shopfast.common.events.NotificationEvent;
import com.shopfast.common.events.OrderCommand;
import com.shopfast.orderservice.client.PaymentRequest;
import com.shopfast.orderservice.client.RemoteGateway;
import com.shopfast.orderservice.dto.CheckoutRequestDto;
import com.shopfast.orderservice.dto.PaymentResponseDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import com.shopfast.orderservice.events.KafkaNotificationProducer;
import com.shopfast.orderservice.events.KafkaOrderProducer;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.model.ProcessedCommand;
import com.shopfast.orderservice.model.ShippingAddress;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import com.shopfast.orderservice.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the checkout flow.
 *
 * <p>Responsibilities intentionally limited to: load cart -> price (with coupon)
 * -> persist order -> take payment -> reserve stock. All post-payment side effects
 * (cart clearing, coupon redemption, stock confirmation) are owned exclusively by
 * {@code KafkaPaymentConsumer} so they happen exactly once.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    /** Monetary values are always stored/compared with 2 decimal places. */
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final OrderRepository orderRepository;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final ProcessedCommandRepository processedCommandRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;
    private final RemoteGateway remoteGateway;
    private final OrderNumberGenerator orderNumberGenerator;

    @Transactional
    public Order checkout(String userId, CheckoutRequestDto request) {
        List<CartItemDto> cartItems = loadCart(userId);

        BigDecimal subTotal = calculateSubTotal(cartItems);
        BigDecimal discount = resolveDiscount(userId, request, cartItems, subTotal);
        BigDecimal total = subTotal.subtract(discount).max(BigDecimal.ZERO).setScale(MONEY_SCALE, MONEY_ROUNDING);

        Order order = buildOrder(userId, request, cartItems, subTotal, discount, total);
        order = orderRepository.save(order); // items cascade from Order

        processPayment(order, request, total);

        publishOrderNotification(order);
        return order;
    }

    // ------------------------------------------------------------------ cart

    private List<CartItemDto> loadCart(String userId) {
        List<CartItemDto> cartItems = remoteGateway.getCart(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        return cartItems;
    }

    // --------------------------------------------------------------- pricing

    /**
     * Sum of line totals. Uses {@link BigDecimal} throughout — never {@code double} —
     * so cent-level rounding errors cannot accumulate.
     */
    private BigDecimal calculateSubTotal(List<CartItemDto> cartItems) {
        return cartItems.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    /**
     * Validates the coupon (if any) and returns the discount, clamped to
     * {@code [0, subTotal]} so a coupon can never produce a negative total.
     */
    private BigDecimal resolveDiscount(String userId,
                                       CheckoutRequestDto request,
                                       List<CartItemDto> cartItems,
                                       BigDecimal subTotal) {
        if (request == null || request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }

        CouponValidateRequestDto validateRequest = new CouponValidateRequestDto();
        validateRequest.setUserId(userId);
        validateRequest.setCode(request.getCouponCode());
        validateRequest.setSubTotal(subTotal.doubleValue());
        validateRequest.setItems(cartItems.stream().map(ci -> {
            CouponLineItemDto line = new CouponLineItemDto();
            line.setProductId(ci.getProductId().toString());
            line.setQuantity(ci.getQuantity());
            line.setPrice(ci.getPrice().doubleValue());
            return line;
        }).toList());

        CouponValidateResponseDto response = remoteGateway.validateCoupon(validateRequest);
        if (response == null || !response.isValid()) {
            String reason = response != null && response.getReason() != null ? response.getReason() : "invalid";
            throw new IllegalArgumentException("Coupon code is invalid: " + reason);
        }

        return BigDecimal.valueOf(response.getDiscount())
                .setScale(MONEY_SCALE, MONEY_ROUNDING)
                .max(BigDecimal.ZERO)
                .min(subTotal);
    }

    // ---------------------------------------------------------------- order

    private Order buildOrder(String userId,
                             CheckoutRequestDto request,
                             List<CartItemDto> cartItems,
                             BigDecimal subTotal,
                             BigDecimal discount,
                             BigDecimal total) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(orderNumberGenerator.next());
        order.setStatus(OrderStatus.CREATED);
        order.setSubTotal(subTotal);
        order.setDiscount(discount);
        order.setTotalAmount(total);
        order.setPaymentStatus(OrderStatus.PENDING);
        order.setPaymentMethod(resolvePaymentMethod(request));

        if (request != null) {
            // Persisting the coupon code is required for redemption after payment succeeds.
            if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                order.setCouponCode(request.getCouponCode());
            }
            order.setShippingAddress(ShippingAddress.builder()
                    .fullName(request.getFullName())
                    .street(request.getStreet())
                    .city(request.getCity())
                    .state(request.getState())
                    .zip(request.getZip())
                    .country(request.getCountry())
                    .phone(request.getPhone())
                    .build());
        }

        List<OrderItem> items = new ArrayList<>(cartItems.size());
        for (CartItemDto cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            items.add(orderItem);
        }
        order.setItems(items);
        return order;
    }

    private PaymentMethod resolvePaymentMethod(CheckoutRequestDto request) {
        return request != null && request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.COD;
    }

    // -------------------------------------------------------------- payment

    private void processPayment(Order order, CheckoutRequestDto request, BigDecimal total) {
        PaymentMethod method = resolvePaymentMethod(request);
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId())
                    .userId(UUID.fromString(order.getUserId()))
                    .amount(total.doubleValue())
                    .method(method)
                    .cardNumber(request != null ? request.getCardNumber() : null)
                    .cardHolderName(request != null ? request.getCardHolderName() : null)
                    .expiryDate(request != null ? request.getExpiryDate() : null)
                    .cvv(request != null ? request.getCvv() : null)
                    .build();

            PaymentResponseDto paymentResponse = remoteGateway.processPayment(order.getUserId(), paymentRequest);
            String paymentStatus = paymentResponse != null ? paymentResponse.getStatus() : null;

            if (method == PaymentMethod.COD) {
                // COD is collected on delivery; the order stays CREATED/PENDING.
                order.setPaymentStatus(OrderStatus.PENDING);
                order.setStatus(OrderStatus.CREATED);
                reserveStock(order);
            } else if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                order.setPaymentStatus(OrderStatus.CONFIRMED);
                order.setStatus(OrderStatus.CONFIRMED);
                reserveStock(order);
            } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
                order.setPaymentStatus(OrderStatus.PAYMENT_FAILED);
                order.setStatus(OrderStatus.PAYMENT_FAILED);
            } else {
                // e.g. Stripe PaymentIntent created — the webhook will settle the status.
                order.setPaymentStatus(OrderStatus.PENDING);
            }
        } catch (Exception ex) {
            log.error("Payment processing failed for order {}: {}", order.getId(), ex.getMessage(), ex);
            order.setPaymentStatus(OrderStatus.PAYMENT_FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            // Persist the failure, then surface it. Dirty checking flushes on commit.
            throw new IllegalStateException("Payment failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Publishes the RESERVE command. The idempotency marker is written in the same
     * transaction as the order so a rollback cannot leave a "processed" record behind.
     */
    private void reserveStock(Order order) {
        String commandId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("userId", order.getUserId());
        payload.put("items", order.getItems().stream()
                .map(i -> Map.<String, Object>of(
                        "productId", i.getProductId().toString(),
                        "quantity", i.getQuantity()))
                .toList());

        OrderCommand command = new OrderCommand();
        command.setCommandId(commandId);
        command.setCommandType("RESERVE");
        command.setOccurredAt(Instant.now());
        command.setPayload(payload);

        processedCommandRepository.save(ProcessedCommand.builder()
                .commandId(commandId)
                .processedAt(Instant.now())
                .build());

        kafkaOrderProducer.publishOrderCommand(command);
        kafkaOrderProducer.reserveOrder(order);
    }

    // --------------------------------------------------------- notification

    private void publishOrderNotification(Order order) {
        NotificationEvent event = new NotificationEvent();
        event.setSubject("Order " + order.getStatus());
        event.setNotificationType(NotificationType.ORDER_CREATED);
        event.setNotificationChannel(NotificationChannel.EMAIL);
        event.setContent("Order " + order.getOrderNumber() + " has been placed with status: " + order.getStatus());
        // Recipient is resolved downstream by notification-service from the userId.
        event.setReferenceId(order.getOrderNumber());
        event.setUserId(order.getUserId());
        event.setEventSource("order-service");
        kafkaNotificationProducer.send(event);
    }
}
