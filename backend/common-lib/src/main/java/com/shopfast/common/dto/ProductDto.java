package com.shopfast.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("slug")
    public String slug;

    @JsonProperty("name")
    public String name;

    @JsonProperty("description")
    public String description;

    @JsonProperty("category_id")
    public String categoryId;

    @JsonProperty("price")
    public BigDecimal price;

    @JsonProperty("currency")
    public String currency;

    @JsonProperty("stock")
    public Integer stock;

    @JsonProperty("rating")
    public Double rating;

    @JsonProperty("images")
    public List<String> images = new ArrayList<>();

    @JsonProperty("tags")
    public List<String> tags = new ArrayList<>();

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_by")
    private String updatedBy;
}
