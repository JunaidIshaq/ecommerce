package com.shopfast.productservice.events;

import com.shopfast.common.events.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaProductProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRODUCT_TOPIC = "product.events";

    public KafkaProductProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishProductEvent(ProductEvent event) {
        kafkaTemplate.send(PRODUCT_TOPIC, event.getEventId(), event)
                .whenComplete((result, error) -> {
                    if(error == null) {
                        log.info("Published product event with id : {}, to partition {} with offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish ProductEvent {} due to {}", event.getEventId(), error.getMessage(), error);
                    }
                });
    }
}
