package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderItemDetailDto {

    @JsonProperty("product_id")
    private UUID productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_slug")
    private String productSlug;

    @JsonProperty("product_description")
    private String productDescription;

    private int quantity;

    private BigDecimal price;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("images")
    private List<String> images;
}
