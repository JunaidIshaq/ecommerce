package com.shopfast.cartservice.util;

import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.OrderResponseDto;
import com.shopfast.cartservice.model.Order;
import com.shopfast.cartservice.model.OrderItem;

public class OrderMapper {


    public static OrderResponseDto getOrderResponseDto(Order order) {
//        OrderResponseDto orderResponseDto = new OrderResponseDto();
//        orderResponseDto.setId(order.getId().toString());
//        orderResponseDto.setUserId(order.getUserId());
//        orderResponseDto.setOrderNumber(order.getOrderNumber());
//        orderResponseDto.setStatus(order.getStatus().toString());
//        orderResponseDto.setUserId(order.getUserId());
//        orderResponseDto.setTotalAmount(String.valueOf(order.getTotalAmount()));
//        orderResponseDto.setItems(order.getItems().stream().map(OrderMapper::getOrderItemDto).toList());
//        return orderResponseDto;
        return null;
    }

//    public static CartItemDto getOrderItemDto(OrderItem orderItem) {
//        CartItemDto cartItemDto = new CartItemDto();
//        cartItemDto.setPrice(orderItem.getPrice());
//        cartItemDto.setProductId(orderItem.getProductId());
//        cartItemDto.setQuantity(orderItem.getQuantity());
//        return cartItemDto;
//    }
}
