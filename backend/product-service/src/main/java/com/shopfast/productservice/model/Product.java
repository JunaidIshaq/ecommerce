package com.shopfast.productservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document("product")
public class Product {

    @Id
    private String id;

    private String slug;

    private String name;

    private String description;

    private String category;

    private BigDecimal price;

    private String currency;

    private Integer stock;

    private Double rating;

    private List<String> images;

    private List<String> tags;

    private Map<String, Object> attributes;

    private Boolean isActive = true;

    private Instant createdAt;

    private Instant updatedAt;

}
