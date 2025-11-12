package com.shopfast.orderservice.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CheckoutRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String couponCode;

}
