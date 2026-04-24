package com.shopfast.common.events;

import lombok.Data;

import java.io.Serializable;

@Data
public class CouponLineItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;

    private Integer quantity;

    private Double price;

}
