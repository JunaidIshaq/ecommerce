package com.shopfast.orderservice.events;

import com.shopfast.common.events.InventoryEvent;
import com.shopfast.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class KafkaInventoryConsumer {

    private final OrderService orderService;

    public KafkaInventoryConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "inventory.events", groupId =  "order-service-group")
    public void onInventoryEvent(InventoryEvent inventoryEvent) {
        try {
            Map<String, Object> payload = inventoryEvent.getPayload();
            String eventType = inventoryEvent.getEventType();
            log.info("Received inventory event: {}, type : {}", inventoryEvent.getEventId(), eventType);
            // pass to orderService
            orderService.handleInventoryEvent(inventoryEvent.getEventId(), eventType, payload);
        } catch (Exception ex) {
            log.error("Error processing inventory event: {}", ex.getMessage(), ex);
            // throwing will let kafka retry depending on container factory config
            throw ex;
        }
    }
}
