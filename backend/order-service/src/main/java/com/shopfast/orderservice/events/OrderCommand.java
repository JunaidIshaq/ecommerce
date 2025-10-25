package com.shopfast.orderservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommand {

    private String commandId;

    private String commandType; //RESERVE | CONFIRM | RELEASE

    private int commandVersion = 1;

    private Instant occurredAt;

    private Map<String, Object> payload;

}
