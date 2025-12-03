package com.shopfast.common.model.elastic;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    private String id;

    private String slug;

    private String name;

    private String description;

    private String categoryId;

    private BigDecimal price;

    private String currency;

    private Integer stock;

    private List<String> images;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

}
