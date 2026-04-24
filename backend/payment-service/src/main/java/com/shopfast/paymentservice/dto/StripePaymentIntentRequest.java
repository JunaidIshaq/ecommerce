package com.shopfast.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePaymentIntentRequest {
    private Long amount;
    private String currency;
    private String description;
    private String paymentMethodTypes;
    private String metadataOrderId;
    private String metadataUserId;
}
