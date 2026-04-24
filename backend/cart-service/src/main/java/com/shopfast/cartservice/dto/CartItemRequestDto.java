package com.shopfast.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CartItemRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String productId;

    @Min(1)
    private Integer quantity = 1;

}
