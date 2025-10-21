package com.shopfast.productservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product")
@JsonIgnoreProperties(ignoreUnknown = true)  // Prevent unknown fields from breaking serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {

    @Id
    private String id;

    private String slug;

    private String name;

    private String description;

    private String categoryId;

    private BigDecimal price;

    private String currency;

    private Integer stock;

    private Double rating;

    private List<String> images;

    private List<String> tags;

    private Map<String, Object> attributes;

    private Boolean isActive = true;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

}
