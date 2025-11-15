package com.shopfast.orderservice.events;

import com.shopfast.common.events.CouponRedeemRequestDto;
import com.shopfast.common.events.PaymentEvent;
import com.shopfast.orderservice.client.CartClient;
import com.shopfast.orderservice.client.CouponClient;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Component
public class KafkaPaymentConsumer {

    private final OrderRepository orderRepository;
    private final RedisProcessedEventStore processedEventStore;
    private final CartClient cartClient;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final CouponClient couponClient;


    public KafkaPaymentConsumer(OrderRepository orderRepository, RedisProcessedEventStore processedEventStore, CartClient cartClient, KafkaOrderProducer kafkaOrderProducer, CouponClient couponClient) {
        this.orderRepository = orderRepository;
        this.processedEventStore = processedEventStore;
        this.cartClient = cartClient;
        this.kafkaOrderProducer = kafkaOrderProducer;
        this.couponClient = couponClient;
    }

    @KafkaListener(topics = "payment.events", groupId = "order-service-group")
    @Transactional
    public void onPaymentEvent(PaymentEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Received null/invalid payment event, skipping");
            return;
        }

        String eventId = event.getEventId();
        boolean first = processedEventStore.markIfNotProcessed(eventId);
        if (!first) {
            log.info("Payment event {} already processed, skipping", eventId);
            return;
        }

        try {
            Map<String, Object> payload = event.getPayload();
            String orderIdStr = (String) payload.get("orderId");
            if (orderIdStr == null) {
                log.warn("Payment event {} has no orderId, skipping", eventId);
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                log.warn("Order {} not found for payment event {}", orderId, eventId);
                return;
            }

            Order order = opt.get();
            String eventType = event.getEventType();
            if ("PAYMENT_SUCCESS".equals(eventType)) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                // Confirm stock
                kafkaOrderProducer.confirmOrder(order);

                // Redeem coupon (if used)
                if (order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
                    try {
                        couponClient.redeem(new CouponRedeemRequestDto() {{
                            setCode(order.getCouponCode());
                            setUserId(order.getUserId().toString());
                        }});
                        log.info("Coupon {} redeemed for user {}", order.getCouponCode(), order.getUserId());
                    } catch (Exception ex) {
                        log.error("Failed to redeem coupon {} for order {}: {}",
                                order.getCouponCode(), orderId, ex.getMessage());
                        // Not fatal — payment succeeded, order confirmed
                    }
                }
                    // Clear cart
                    try {
                        cartClient.clearCartInternal(order.getUserId());
                        log.info("Cart cleared for user {}", order.getUserId());
                    } catch (Exception ex) {
                        log.error("Failed to clear cart for user {}: {}", order.getUserId(), ex.getMessage());
                    }
                    // optionally set payment reference, etc.
                    log.info("Order {} confirmed", orderId);
            } else if ("PAYMENT_FAILED".equals(eventType)) {
                log.warn("Payment FAILED for order {}", orderId);

                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                // Release reserved stock
                kafkaOrderProducer.releaseOrder(order);
            } else if ("PAYMENT_REFUNDED".equals(eventType)) {
                order.setStatus(OrderStatus.REFUNDED);
            } else {
                log.info("Unhandled payment event type {} for event {}", eventType, eventId);
            }
            log.info("Order {} updated to {} due to payment event {}", orderId, order.getStatus(), eventId);

        } catch (Exception ex) {
            // Let exception bubble up and be handled by Kafka error handler (retries/DLQ)
            log.error("Failed to process payment event {} : {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
