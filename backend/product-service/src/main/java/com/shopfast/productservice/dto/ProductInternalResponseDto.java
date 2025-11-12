package com.shopfast.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductInternalResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String title;

    private BigDecimal price;

    private Boolean active;

}