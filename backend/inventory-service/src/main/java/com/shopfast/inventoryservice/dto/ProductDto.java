package com.shopfast.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;

    public String slug;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    public String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    public String description;

    @NotBlank(message = "CategoryId is required")
    public String categoryId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    public BigDecimal price;

    public String currency;

    @NotNull(message = "Stock is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    public Integer stock;

    public Double rating;

    public List<String> images;

    public List<String> tags;

    private String createdAt;

    private String updatedAt;

    private String createdBy;

    private String updatedBy;

}
