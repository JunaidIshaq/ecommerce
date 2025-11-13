package com.shopfast.couponservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartItemRequestDto {

    @NotBlank
    private String productId;

    @Min(1)
    private Integer quantity = 1;

}
