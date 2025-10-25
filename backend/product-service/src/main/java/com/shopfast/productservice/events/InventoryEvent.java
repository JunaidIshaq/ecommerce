package com.shopfast.productservice.events;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class InventoryEvent {

    private String eventId;

    private String eventType;

    private int eventVersion;

    private Instant occurredAt;

    private Map<String, Object> payload;

}