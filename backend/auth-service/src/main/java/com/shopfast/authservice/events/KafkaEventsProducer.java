package com.shopfast.authservice.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class KafkaEventsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventsProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String TOPIC = "auth.events";

    public void publish(String eventType, Map<String, Object> payload) {
        var event = Map.<String, Object>of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", eventType,
                "payload", payload);
        kafkaTemplate.send(TOPIC, event.get("eventId").toString(), event)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        log.info("Published auth event {}", eventType);
                    } else {
                        log.error("Failed to publish auth event  : {} ", error.getMessage());
                    }
        });
    }
}
