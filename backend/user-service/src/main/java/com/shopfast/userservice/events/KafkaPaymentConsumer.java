package com.shopfast.userservice.events;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.userservice.enums.OrderStatus;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final RedisProcessedEventStore processedEventStore;

    public KafkaPaymentConsumer(UserRepository userRepository, RedisProcessedEventStore processedEventStore) {
        this.userRepository = userRepository;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = "payment.events", groupId = "user-service-group")
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
            Optional<User> opt = userRepository.findById(orderId);
            if (opt.isEmpty()) {
                log.warn("User {} not found for payment event {}", orderId, eventId);
                return;
            }

            User user = opt.get();
//            String eventType = event.getEventType();
//            if ("PAYMENT_SUCCESS".equals(eventType)) {
//                user.setStatus(OrderStatus.CONFIRMED);
//                // optionally set payment reference, etc.
//            } else if ("PAYMENT_FAILED".equals(eventType)) {
//                user.setStatus(OrderStatus.CANCELLED);
//            } else if ("PAYMENT_REFUNDED".equals(eventType)) {
//                user.setStatus(OrderStatus.REFUNDED);
//            } else {
//                log.info("Unhandled payment event type {} for event {}", eventType, eventId);
//            }

            userRepository.save(user);
            log.info("User {} updated to {} due to payment event {}", orderId, user.getStatus(), eventId);

        } catch (Exception ex) {
            // Let exception bubble up and be handled by Kafka error handler (retries/DLQ)
            log.error("Failed to process payment event {} : {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
