package com.shopfast.inventoryservice.util;

import com.shopfast.inventoryservice.dto.InventoryRequestDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
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
}
