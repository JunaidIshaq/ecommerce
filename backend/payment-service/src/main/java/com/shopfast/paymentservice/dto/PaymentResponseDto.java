package com.shopfast.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.paymentservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("status")
    private PaymentStatus status;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

}