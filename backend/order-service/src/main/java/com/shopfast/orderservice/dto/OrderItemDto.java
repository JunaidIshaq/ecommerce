package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
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

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_slug")
    private String productSlug;

    @JsonProperty("product_description")
    private String productDescription;

    @JsonProperty("image_url")
    private String imageUrl;

    private List<String> images;

}

