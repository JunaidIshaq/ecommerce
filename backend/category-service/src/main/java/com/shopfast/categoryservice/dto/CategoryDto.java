package com.shopfast.categoryservice.dto;

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
public class CategoryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;


    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    public String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    public String description;

    public String parentId;

    public List<String> subCategoryIds;

    private String createdAt;

    private String updatedAt;

    private String createdBy;

    private String updatedBy;

}
