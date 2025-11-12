package com.shopfast.orderservice.events;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.orderservice.client.CartClient;
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

    public KafkaPaymentConsumer(OrderRepository orderRepository, RedisProcessedEventStore processedEventStore, CartClient cartClient, KafkaOrderProducer kafkaOrderProducer) {
        this.orderRepository = orderRepository;
        this.processedEventStore = processedEventStore;
        this.cartClient = cartClient;
        this.kafkaOrderProducer = kafkaOrderProducer;
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
                cartClient.clearCartInternal(order.getUserId());
                // optionally set payment reference, etc.
            } else if ("PAYMENT_FAILED".equals(eventType)) {
                order.setStatus(OrderStatus.CANCELLED);
            } else if ("PAYMENT_REFUNDED".equals(eventType)) {
                order.setStatus(OrderStatus.REFUNDED);
            } else {
                log.info("Unhandled payment event type {} for event {}", eventType, eventId);
            }
            orderRepository.save(order);
            kafkaOrderProducer.releaseOrder(order);
            log.info("Order {} updated to {} due to payment event {}", orderId, order.getStatus(), eventId);

        } catch (Exception ex) {
            // Let exception bubble up and be handled by Kafka error handler (retries/DLQ)
            log.error("Failed to process payment event {} : {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
