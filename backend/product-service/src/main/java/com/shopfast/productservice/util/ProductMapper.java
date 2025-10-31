package com.shopfast.productservice.util;

import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.model.Product;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.UUID;

public class ProductMapper {

    public static Product getProduct(ProductDto dto) {
        Product product = new Product();
        product.setId(UUID.fromString(dto.getId()));
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setImages(dto.getImages());
        product.setCreatedAt(Instant.parse(dto.getCreatedAt()));
        product.setUpdatedAt(Instant.parse(dto.getUpdatedAt()));
        product.setCreatedBy(dto.getCreatedBy());
        product.setUpdatedBy(dto.getUpdatedBy());
        return product;
    }

    public static ProductDto getProductDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(String.valueOf(product.getId()));
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategoryId());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setImages(product.getImages());
        dto.setCreatedAt(String.valueOf(product.getCreatedAt()));
        dto.setUpdatedAt(String.valueOf(product.getUpdatedAt()));
        dto.setCreatedBy(product.getCreatedBy());
        dto.setUpdatedBy(product.getUpdatedBy());
        return dto;
    }

    public static Product createProduct(@Valid ProductDto productDto) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setSlug(productDto.getSlug());
        product.setDescription(productDto.getDescription());
        product.setCategoryId(productDto.getCategoryId());
        product.setPrice(productDto.getPrice());
        product.setStock(productDto.getStock());
        product.setImages(productDto.getImages());
        return product;
    }
}
