package com.shopfast.categoryservice.util;

import com.shopfast.categoryservice.dto.CategoryDto;
import com.shopfast.categoryservice.model.Category;

public class CategoryMapper {

    public static Category getCategory(CategoryDto dto) {
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setParentId(dto.getParentId());
        category.setSubCategoryIds(dto.getSubCategoryIds());
        return category;
    }

    public static CategoryDto getCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setParentId(category.getParentId());
        dto.setSubCategoryIds(category.getSubCategoryIds());
        return dto;
    }

}
