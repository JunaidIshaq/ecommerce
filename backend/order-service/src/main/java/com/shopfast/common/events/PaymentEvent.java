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
public class PaymentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType; // PAYMENT_SUCCESS | PAYMENT_FAILED | PAYMENT_REFUNDED

    private int eventVersion = 1;

    private Instant occurredAt;

    private Map<String, Object> payload;

}
