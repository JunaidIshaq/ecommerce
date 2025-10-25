package com.shopfast.inventoryservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {

    private String eventId;

    private String eventType;      // INVENTORY_RESERVED, INVENTORY_RELEASED, INVENTORY_CONFIRMED, INVENTORY_ADJUSTED

    private int eventVersion;

    private Instant occurredAt;

    private Map<String, Object> payload;

}