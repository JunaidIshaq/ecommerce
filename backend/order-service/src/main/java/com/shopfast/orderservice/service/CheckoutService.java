package com.shopfast.orderservice.service;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.common.enums.NotificationType;
import com.shopfast.common.events.CartItemDto;
import com.shopfast.common.events.CouponLineItemDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.NotificationEvent;
import com.shopfast.common.events.OrderCommand;
import com.shopfast.orderservice.client.CartClient;
import com.shopfast.orderservice.client.CouponClient;
import com.shopfast.orderservice.client.PaymentClient;
import com.shopfast.orderservice.client.PaymentRequest;
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
import com.shopfast.orderservice.repository.OrderItemRepository;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartClient cartClient;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final ProcessedCommandRepository processedCommandRepository;
    private final CouponClient couponClient;
    private final KafkaNotificationProducer kafkaNotificationProducer;
    private final PaymentClient paymentClient;

    public CheckoutService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, CartClient cartClient, KafkaOrderProducer kafkaOrderProducer, ProcessedCommandRepository processedCommandRepository, CouponClient couponClient, KafkaNotificationProducer kafkaNotificationProducer, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartClient = cartClient;
        this.kafkaOrderProducer = kafkaOrderProducer;
        this.processedCommandRepository = processedCommandRepository;
        this.couponClient = couponClient;
        this.kafkaNotificationProducer = kafkaNotificationProducer;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public Order checkout(String userId, CheckoutRequestDto checkoutRequestDto) {
        // 1) Load Cart
        var cartItems = cartClient.getCartInternal(userId);
        if(cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // 2) Price calculation (basic; coupons integrated in step 3)
        double subTotal = cartItems.stream().mapToDouble(i -> i.getPrice().doubleValue() * i.getQuantity())
                .sum();
        double discount = 0.0;
        double total = Math.max(9, subTotal - discount);
        // 3) Persist order + items
        Order order = new Order();
        if(checkoutRequestDto != null) {
            if(checkoutRequestDto.getCouponCode() != null && !checkoutRequestDto.getCouponCode().isBlank()){
            CouponValidateRequestDto requestDto = new CouponValidateRequestDto();
            requestDto.setUserId(userId);
            requestDto.setCode(checkoutRequestDto.getCouponCode());
            requestDto.setSubTotal(subTotal);
            requestDto.setItems(cartItems.stream().map(ci -> {
                CouponLineItemDto couponLineItemDto = new CouponLineItemDto();
                couponLineItemDto.setProductId(ci.getProductId().toString());
                couponLineItemDto.setQuantity(ci.getQuantity());
                couponLineItemDto.setPrice(ci.getPrice().doubleValue());
                return couponLineItemDto;
            }).toList());
            var response = couponClient.validate(requestDto);
            if (response.isValid()) {
                discount = response.getDiscount();
                total = Math.max(total, subTotal - discount);
            } else {
                throw new IllegalArgumentException("Coupon code is invalid");
            }
        }
            ShippingAddress shippingAddress = ShippingAddress.builder()
                    .fullName(checkoutRequestDto.getFullName())
                    .street(checkoutRequestDto.getStreet())
                    .city(checkoutRequestDto.getCity())
                    .state(checkoutRequestDto.getState())
                    .zip(checkoutRequestDto.getZip())
                    .country(checkoutRequestDto.getCountry())
                    .phone(checkoutRequestDto.getPhone())
                    .build();
            order.setShippingAddress(shippingAddress);
        }
        String orderNumber = "Order-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();


        order.setUserId(userId);
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.CREATED);
        order.setSubTotal(BigDecimal.valueOf(subTotal));
        order.setDiscount(BigDecimal.valueOf(discount));
        order.setTotalAmount(BigDecimal.valueOf(total));

        // Set payment method and initial payment status
        if (checkoutRequestDto != null && checkoutRequestDto.getPaymentMethod() != null) {
            order.setPaymentMethod(checkoutRequestDto.getPaymentMethod());
            order.setPaymentStatus(OrderStatus.PENDING);
        } else {
            order.setPaymentMethod(PaymentMethod.COD);
            order.setPaymentStatus(OrderStatus.PENDING);
        }

        List<OrderItem> items = new ArrayList<>();
        for(CartItemDto cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            items.add(orderItem);
        }

        orderItemRepository.saveAll(items);
        order.setItems(items);

        order = orderRepository.save(order);

        // 4) Process payment
        try {
            // Build payment request for payment-service
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId())
                    .userId(UUID.fromString(userId))
                    .amount(total)
                    .method(checkoutRequestDto != null && checkoutRequestDto.getPaymentMethod() != null
                            ? checkoutRequestDto.getPaymentMethod()
                            : PaymentMethod.COD)
                    .cardNumber(checkoutRequestDto != null ? checkoutRequestDto.getCardNumber() : null)
                    .cardHolderName(checkoutRequestDto != null ? checkoutRequestDto.getCardHolderName() : null)
                    .expiryDate(checkoutRequestDto != null ? checkoutRequestDto.getExpiryDate() : null)
                    .cvv(checkoutRequestDto != null ? checkoutRequestDto.getCvv() : null)
                    .build();

            PaymentResponseDto paymentResponse = paymentClient.processPayment(userId, paymentRequest);

            // Update order with payment status
            if ("SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
                order.setPaymentStatus(OrderStatus.CONFIRMED);
                order.setStatus(OrderStatus.CONFIRMED);
            } else if ("FAILED".equalsIgnoreCase(paymentResponse.getStatus())) {
                order.setPaymentStatus(OrderStatus.PAYMENT_FAILED);
                order.setStatus(OrderStatus.PAYMENT_FAILED);
            } else {
                order.setPaymentStatus(OrderStatus.PENDING);
            }
            orderRepository.save(order);

            // If payment succeeded, proceed with inventory reservation
            if ("SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
                // Publish RESERVE command for each item
                String commandId = UUID.randomUUID().toString();
                OrderCommand orderCommand = new OrderCommand();
                orderCommand.setCommandId(commandId);
                orderCommand.setCommandType("RESERVE");
                orderCommand.setOccurredAt(Instant.now());
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getId().toString());
                payload.put("userId", order.getUserId());

                payload.put("items", order.getItems().stream().map(i -> Map.of(
                        "productId", i.getProductId().toString(),
                        "quantity", i.getQuantity()
                )).toList());
                orderCommand.setPayload(payload);

                processedCommandRepository.save(ProcessedCommand.builder()
                        .commandId(commandId)
                        .processedAt(Instant.now())
                        .build());

                kafkaOrderProducer.publishOrderCommand(orderCommand);
                kafkaOrderProducer.reserveOrder(order);

                // Clear cart
                try {
                    cartClient.clearCartInternal(order.getUserId());
                    log.info("Cart cleared for user {}", order.getUserId());
                } catch (Exception ex) {
                    log.error("Failed to clear cart for user {}: {}", order.getUserId(), ex.getMessage());
                }
            } else if ("FAILED".equalsIgnoreCase(paymentResponse.getStatus())) {
                // Payment failed - release inventory if reserved
                kafkaOrderProducer.releaseOrder(order);
            }

        } catch (Exception ex) {
            log.error("Payment processing failed for order {}: {}", order.getId(), ex.getMessage());
            order.setPaymentStatus(OrderStatus.PAYMENT_FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw new IllegalArgumentException("Payment failed: " + ex.getMessage());
        }

        // Notification Producer -> Notification Service
        NotificationEvent notificationEvent = new NotificationEvent();
        notificationEvent.setSubject("Order " + order.getStatus());
        notificationEvent.setNotificationType(NotificationType.ORDER_CREATED);
        notificationEvent.setNotificationChannel(NotificationChannel.EMAIL);
        notificationEvent.setContent("Order has been placed with status: " + order.getStatus());
        notificationEvent.setRecipient("junaidnumlcs@gmail.com");
        notificationEvent.setReferenceId(order.getOrderNumber());
        notificationEvent.setUserId(userId);
        notificationEvent.setEventSource("order-service");
        kafkaNotificationProducer.send(notificationEvent);

        return order;
    }
}
