package com.shopfast.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePaymentIntentResponse {
    private String id;
    private String clientSecret;
    private String status;
    private Long amount;
    private String currency;
}
