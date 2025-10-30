package com.shopfast.orderservice.util;

import com.shopfast.orderservice.dto.OrderItemDto;
import com.shopfast.orderservice.model.OrderItem;

public class OrderMapper {


    public static OrderItemDto getOrderDto(OrderItem orderItem) {
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setPrice(orderItem.getPrice());
        orderItemDto.setProductId(orderItem.getProductId());
        orderItemDto.setQuantity(orderItem.getQuantity());
        return orderItemDto;
    }
}
