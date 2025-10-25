package com.shopfast.inventoryservice.events;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class OrderCommand {

    private String commandId;

    private String commandType;     //RESERVE | CONFIRM | RELEASE

    private int commandVersion = 1;

    private Instant occurredAt;

    private Map<String, Object> payload;

}