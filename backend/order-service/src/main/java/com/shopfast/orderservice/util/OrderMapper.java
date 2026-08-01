package com.shopfast.orderservice.util;

import com.shopfast.orderservice.dto.OrderItemDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;

public class OrderMapper {


    public static OrderResponseDto getOrderResponseDto(Order order) {
        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setId(order.getId().toString());
        orderResponseDto.setUserId(order.getUserId());
        orderResponseDto.setOrderNumber(order.getOrderNumber());
        orderResponseDto.setStatus(order.getStatus().toString());
        orderResponseDto.setUserId(order.getUserId());
        orderResponseDto.setSubTotal(String.valueOf(order.getSubTotal()));
        orderResponseDto.setDiscount(order.getDiscount() != null ? order.getDiscount().toString() : "0");
        orderResponseDto.setTotalAmount(String.valueOf(order.getTotalAmount()));
        orderResponseDto.setPaymentMethod(order.getPaymentMethod());
        orderResponseDto.setPaymentStatus(order.getPaymentStatus());
        orderResponseDto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        orderResponseDto.setUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);
        orderResponseDto.setItems(order.getItems().stream().map(OrderMapper::getOrderItemDto).toList());
        return orderResponseDto;
    }

    public static OrderItemDto getOrderItemDto(OrderItem orderItem) {
       return OrderItemDto.builder().price(orderItem.getPrice())
                        .productId(orderItem.getProductId())
                                .quantity(orderItem.getQuantity()).build();

    }
}
