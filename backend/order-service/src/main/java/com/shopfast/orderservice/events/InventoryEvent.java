package com.shopfast.orderservice.events;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class InventoryEvent {

    private String eventId;

    private String eventType; // INVENTORY_RESERVED | INVENTORY_CONFIRMED | INVENTORY_RELEASED | INVENTORY_FAILED

    private int eventVersion = 1;

    private Instant occurredAt;

    private Map<String, Object> payload;

}
