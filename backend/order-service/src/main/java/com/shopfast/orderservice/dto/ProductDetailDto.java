package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailDto {

    private UUID id;

    private String name;

    private String slug;

    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    private List<String> images;

    private BigDecimal price;
}
