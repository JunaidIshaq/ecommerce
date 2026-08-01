package com.shopfast.orderservice.client;

import com.shopfast.orderservice.enums.PaymentMethod;
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
public class PaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID orderId;

    private UUID userId;

    private double amount;

    private PaymentMethod method;

    private String cardNumber;

    private String cardHolderName;

    private String expiryDate;

    private String cvv;
}
