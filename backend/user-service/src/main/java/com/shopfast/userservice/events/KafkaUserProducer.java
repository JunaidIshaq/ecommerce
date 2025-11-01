package com.shopfast.userservice.events;

import com.shopfast.common.events.OrderCommand;
import com.shopfast.userservice.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class KafkaUserProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaUserProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final String TOPIC = "user.events";

    public void publishUserRegistered(User user) {
        var ev = Map.<String, Object>of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "USER_REGISTERED",
                "payload", Map.of(
                        "userId", user.getId().toString(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        );
        kafkaTemplate.send(TOPIC, user.getId().toString(), user)
                .whenComplete((result, error) -> {
                    if (error == null) {
                         log.info("Published USER_REGISTERED event for user {}", user.getEmail());
                    } else {
                        log.error("Failed to publish USER_REGISTERED : {}", error.getMessage());
                    }
        });
    }
}
