package com.shopfast.orderservice.events;

import com.shopfast.common.events.CouponRedeemRequestDto;
import com.shopfast.common.events.PaymentEvent;
import com.shopfast.orderservice.client.RemoteGateway;
import com.shopfast.orderservice.domain.OrderStatusTransitions;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Single owner of all post-payment side effects: status transition, stock
 * confirmation/release, coupon redemption and cart clearing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentConsumer {

    private final OrderRepository orderRepository;
    private final RedisProcessedEventStore processedEventStore;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final RemoteGateway remoteGateway;

    @KafkaListener(topics = "payment.events", groupId = "order-service-group")
    @Transactional
    public void onPaymentEvent(PaymentEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Received null/invalid payment event, skipping");
            return;
        }

        String eventId = event.getEventId();
        if (!processedEventStore.markIfNotProcessed(eventId)) {
            log.info("Payment event {} already processed, skipping", eventId);
            return;
        }

        Map<String, Object> payload = event.getPayload();
        if (payload == null || payload.get("orderId") == null) {
            log.warn("Payment event {} has no orderId, skipping", eventId);
            return;
        }

        UUID orderId;
        try {
            orderId = UUID.fromString(String.valueOf(payload.get("orderId")));
        } catch (IllegalArgumentException ex) {
            log.warn("Payment event {} carries a malformed orderId '{}', discarding", eventId, payload.get("orderId"));
            return; // non-retryable: do not poison the partition
        }

        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            log.warn("Order {} not found for payment event {}", orderId, eventId);
            return;
        }

        Order order = maybeOrder.get();
        String eventType = event.getEventType();

        switch (eventType == null ? "" : eventType) {
            case "PAYMENT_SUCCESS" -> handleSuccess(order, orderId);
            case "PAYMENT_FAILED" -> handleFailure(order, orderId);
            case "PAYMENT_REFUNDED" -> handleRefund(order, orderId);
            default -> log.info("Unhandled payment event type {} for event {}", eventType, eventId);
        }

        log.info("Order {} is {} after payment event {}", orderId, order.getStatus(), eventId);
    }

    private void handleSuccess(Order order, UUID orderId) {
        if (!OrderStatusTransitions.canApply(orderId, order.getStatus(), OrderStatus.CONFIRMED)) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        kafkaOrderProducer.confirmOrder(order);
        redeemCoupon(order, orderId);
        clearCart(order);
    }

    private void handleFailure(Order order, UUID orderId) {
        if (!OrderStatusTransitions.canApply(orderId, order.getStatus(), OrderStatus.PAYMENT_FAILED)) {
            return;
        }
        log.warn("Payment FAILED for order {}", orderId);
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setPaymentStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        kafkaOrderProducer.releaseOrder(order);
    }

    private void handleRefund(Order order, UUID orderId) {
        if (!OrderStatusTransitions.canApply(orderId, order.getStatus(), OrderStatus.REFUNDED)) {
            return;
        }
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(OrderStatus.REFUNDED);
        orderRepository.save(order); // previously missing — the refund was never persisted

        kafkaOrderProducer.releaseOrder(order);
    }

    private void redeemCoupon(Order order, UUID orderId) {
        if (order.getCouponCode() == null || order.getCouponCode().isBlank()) {
            return;
        }
        try {
            CouponRedeemRequestDto redeemRequest = new CouponRedeemRequestDto();
            redeemRequest.setCode(order.getCouponCode());
            redeemRequest.setUserId(order.getUserId());
            remoteGateway.redeemCoupon(redeemRequest);
            log.info("Coupon {} redeemed for user {}", order.getCouponCode(), order.getUserId());
        } catch (Exception ex) {
            // Not fatal: payment succeeded and the order is confirmed.
            log.error("Failed to redeem coupon {} for order {}: {}",
                    order.getCouponCode(), orderId, ex.getMessage());
        }
    }

    private void clearCart(Order order) {
        // The gateway already degrades quietly here; this guard only stops an
        // unexpected error from rolling back an otherwise-confirmed order.
        try {
            remoteGateway.clearCart(order.getUserId(), order.isGuest());
            log.info("Cart cleared for user {}", order.getUserId());
        } catch (Exception ex) {
            log.error("Failed to clear cart for user {}: {}", order.getUserId(), ex.getMessage());
        }
    }
}
