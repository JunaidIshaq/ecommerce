package com.shopfast.productservice.util;

import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.ProductInternalResponseDto;
import com.shopfast.productservice.model.Product;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProductMapper {

    public static Product getProduct(ProductDto dto) {
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setImages(dto.getImages());
        product.setCreatedAt(dto.getCreatedAt());
        product.setUpdatedAt(dto.getUpdatedAt());
        product.setCreatedBy(dto.getCreatedBy());
        product.setUpdatedBy(dto.getUpdatedBy());
        return product;
    }

    public static ProductDto getProductDto(Product product) {
        if (product == null) {
            return null;
        }
        
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSlug(product.getSlug());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategoryId());
        dto.setPrice(product.getPrice());
        dto.setRating(product.getRating());
        dto.setStock(product.getStock());
        dto.setImages(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setCreatedBy(product.getCreatedBy());
        dto.setUpdatedBy(product.getUpdatedBy());
        return dto;
    }

    public static ProductInternalResponseDto getProductInternalDto(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductInternalResponseDto.builder()
                .id(String.valueOf(product.getId()))
                .title(product.getName())
                .active(product.getIsActive())
                .price(product.getPrice())
                .images(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>())
                .build();
    }

    public static Product createProduct(ProductDto productDto) {
        if (productDto == null) {
            return null;
        }
        
        Product product = new Product();
        product.setName(productDto.getName());
        product.setSlug(productDto.getSlug());
        product.setDescription(productDto.getDescription());
        product.setCategoryId(productDto.getCategoryId());
        product.setPrice(productDto.getPrice());
        product.setRating(productDto.getRating());
        product.setStock(productDto.getStock());
        product.setImages(productDto.getImages() != null ? new ArrayList<>(productDto.getImages()) : new ArrayList<>());
        return product;
    }
}
