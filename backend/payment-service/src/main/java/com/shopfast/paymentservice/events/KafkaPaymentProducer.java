package com.shopfast.paymentservice.events;

import com.shopfast.common.events.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
public class KafkaPaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENTS = "payment.events";

    public KafkaPaymentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(TOPIC_PAYMENTS, event.getEventId(), event)
                .whenComplete((result, error) -> {
                    if(error != null) {
                        log.info("Published payment event with id : {}, to partition {} with offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish event {} due to {}", event.getEventId(), error.getMessage(), error);
                    }
                });
    }
}
