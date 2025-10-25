package com.shopfast.orderservice.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaOrderProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final String ORDER_COMMANDS_TOPIC = "order.commands";

    public void publishOrderCommand(OrderCommand orderCommand) {
        kafkaTemplate.send(ORDER_COMMANDS_TOPIC, orderCommand.getCommandId(), orderCommand)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        log.info("Published OrderCommand {} to partition {} with offset {}",
                                orderCommand.getCommandId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderCommand {} due to {}", orderCommand.getCommandId(), error.getMessage(), error);
                    }
        });
    }
}
