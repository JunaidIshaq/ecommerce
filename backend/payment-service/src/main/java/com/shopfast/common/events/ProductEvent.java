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
public class ProductEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;     // PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED

    private int eventVersion;

    private Instant occurredAt;

    private Map<String, Object> payload;

}
