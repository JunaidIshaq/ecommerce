package com.shopfast.paymentservice.dto;

import com.shopfast.paymentservice.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class PaymentRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private UUID orderId;

    @NotNull
    private UUID userId;

    @Min(0)
    private double amount;

    @NotNull
    private PaymentMethod method;

}
