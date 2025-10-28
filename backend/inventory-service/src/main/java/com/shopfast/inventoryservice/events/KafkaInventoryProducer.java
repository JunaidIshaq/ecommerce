package com.shopfast.inventoryservice.events;

import com.shopfast.common.events.InventoryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaInventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String INVENTORY_TOPIC = "inventory.events";

    public KafkaInventoryProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInventoryEvent(InventoryEvent inventoryEvent) {
        kafkaTemplate.send(INVENTORY_TOPIC, inventoryEvent.getEventId(), inventoryEvent)
                .whenComplete((result, error) -> {
                    if(error != null) {
                        log.info("Published inventory event with id : {}, to partition {} with offset {}",
                                inventoryEvent.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish inventoryEvent {} due to {}", inventoryEvent.getEventId(), error.getMessage(), error);
                    }
                });
    }
}
