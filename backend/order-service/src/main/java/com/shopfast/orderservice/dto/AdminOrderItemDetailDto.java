package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderItemDetailDto {

    @JsonProperty("product_id")
    private UUID productId;

    private int quantity;

    private BigDecimal price;

    @JsonProperty("image_url")
    private String imageUrl;
}
