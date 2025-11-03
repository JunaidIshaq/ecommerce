package com.shopfast.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @JsonProperty("product_id")
    private UUID productId;

    @Min(1)
    private int quantity;

    @NotNull
    private BigDecimal price;

}
