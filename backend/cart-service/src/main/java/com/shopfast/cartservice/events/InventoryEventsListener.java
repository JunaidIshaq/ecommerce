package com.shopfast.cartservice.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryEventsListener {

//    private final CartService cartService;
//
//    public InventoryEventsListener(CartService cartService) {
//        this.cartService = cartService;
//    }
//
//    @KafkaListener(topics = "inventory.events", groupId =  "order-service-group")
//    public void onInventoryEvent(InventoryEvent inventoryEvent) {
//        try {
//            Map<String, Object> payload = inventoryEvent.getPayload();
//            String eventType = inventoryEvent.getEventType();
//            log.info("Received inventory event: {}, type : {}", inventoryEvent.getEventId(), eventType);
//            // pass to cartService
//            cartService.handleInventoryEvent(inventoryEvent.getEventId(), eventType, payload);
//        } catch (Exception ex) {
//            log.error("Error processing inventory event: {}", ex.getMessage(), ex);
//            // throwing will let kafka retry depending on container factory config
//            throw ex;
//        }
//    }
}
