package com.shopfast.couponservice.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductInternalResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String title;

    private BigDecimal price;

    private Boolean active;

}
