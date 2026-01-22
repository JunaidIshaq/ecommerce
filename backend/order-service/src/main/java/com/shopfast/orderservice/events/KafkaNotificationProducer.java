package com.shopfast.orderservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.common.events.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaNotificationProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(NotificationEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("notification-topic", message);
            log.info("Notification event published to topic: {}", message);
        } catch (Exception ex) {
            log.error("Failed to publish notification event", ex);
        }
    }
}
