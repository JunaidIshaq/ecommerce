package com.shopfast.orderservice.util;

import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.model.Order;

public class InventoryMapper {

    public static OrderResponseDto getInventoryResponseDto(Order order) {
        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setId(order.getId().toString());
        orderResponseDto.setProductId(order.getProductId().toString());
        orderResponseDto.setAvailableQuantity(order.getAvailableQuantity());
        orderResponseDto.setReservedQuantity(order.getReservedQuantity());
        orderResponseDto.setSoldQuantity(order.getSoldQuantity());
        orderResponseDto.setCreatedAt(order.getCreatedAt().toString());
        orderResponseDto.setUpdatedAt(order.getUpdatedAt().toString());
        return orderResponseDto;
    }
}
