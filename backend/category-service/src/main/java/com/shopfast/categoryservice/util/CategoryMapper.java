package com.shopfast.categoryservice.util;

import com.shopfast.categoryservice.dto.CategoryDto;
import com.shopfast.categoryservice.model.Category;

public class CategoryMapper {

    public static Category getCategory(CategoryDto dto) {
        Category product = new Category();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setParentId(dto.getParentId());
        product.setSubCategoryIds(dto.getSubCategoryIds());
        return product;
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
