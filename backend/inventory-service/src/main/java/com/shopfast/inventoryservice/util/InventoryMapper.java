package com.shopfast.inventoryservice.util;

import com.shopfast.common.dto.ProductDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
import com.shopfast.inventoryservice.dto.InventoryWithProductDto;
import com.shopfast.inventoryservice.model.InventoryItem;

public class InventoryMapper {

    public static InventoryResponseDto getInventoryResponseDto(InventoryItem inventoryItem) {
        InventoryResponseDto inventoryResponseDto = new InventoryResponseDto();
        inventoryResponseDto.setId(inventoryItem.getId().toString());
        inventoryResponseDto.setProductId(inventoryItem.getProductId().toString());
        inventoryResponseDto.setAvailableQuantity(inventoryItem.getAvailableQuantity());
        inventoryResponseDto.setReservedQuantity(inventoryItem.getReservedQuantity());
        inventoryResponseDto.setSoldQuantity(inventoryItem.getSoldQuantity());
        inventoryResponseDto.setCreatedAt(inventoryItem.getCreatedAt().toString());
        inventoryResponseDto.setUpdatedAt(inventoryItem.getUpdatedAt().toString());
        return inventoryResponseDto;
    }

    public static InventoryWithProductDto getInventoryWithProductDto(InventoryItem inventoryItem, ProductDto productDto) {
        return InventoryWithProductDto.builder()
                .id(inventoryItem.getId().toString())
                .productId(inventoryItem.getProductId().toString())
                .availableQuantity(inventoryItem.getAvailableQuantity())
                .reservedQuantity(inventoryItem.getReservedQuantity())
                .soldQuantity(inventoryItem.getSoldQuantity())
                .createdAt(inventoryItem.getCreatedAt().toString())
                .updatedAt(inventoryItem.getUpdatedAt().toString())
                .product(productDto)
                .build();
    }
}
