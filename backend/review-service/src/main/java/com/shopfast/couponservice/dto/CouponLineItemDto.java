package com.shopfast.couponservice.dto;

import lombok.Data;

@Data
public class CouponLineItemDto {

    private String productId;

    private Integer quantity;

    private Double price;

}
