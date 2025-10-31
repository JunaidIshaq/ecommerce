package com.shopfast.categoryservice.util;

import com.shopfast.categoryservice.dto.CategoryDto;
import com.shopfast.categoryservice.model.Category;

import java.util.Locale;
import java.util.UUID;

public class CategoryMapper {

    public static Category getCategory(CategoryDto dto) {
        Category category = new Category();
        category.setId(UUID.fromString(dto.getId()));
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setParentId(dto.getParentId());
        category.setSubCategoryIds(dto.getSubCategoryIds());
        return category;
    }

    public static CategoryDto getCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .createdAt(category.getCreatedAt() == null ? null : category.getCreatedAt().toString())
                .updatedAt(category.getUpdatedAt() == null ? null : category.getUpdatedAt().toString())
                .build();
    }

}
