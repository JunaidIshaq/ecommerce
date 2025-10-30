package com.shopfast.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String commandId;

    private String commandType; //RESERVE | CONFIRM | RELEASE

    private int commandVersion = 1;

    private Instant occurredAt;

    private Map<String, Object> payload;

}
