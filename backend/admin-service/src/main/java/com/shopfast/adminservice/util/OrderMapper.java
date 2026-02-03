//package com.shopfast.adminservice.util;
//
//import com.shopfast.adminservice.dto.OrderItemDto;
//import com.shopfast.adminservice.dto.OrderResponseDto;
//import com.shopfast.adminservice.model.Order;
//import com.shopfast.adminservice.model.OrderItem;
//
//public class OrderMapper {
//
//
//    public static OrderResponseDto getOrderResponseDto(Order order) {
//        OrderResponseDto orderResponseDto = new OrderResponseDto();
//        orderResponseDto.setId(order.getId().toString());
//        orderResponseDto.setUserId(order.getUserId());
//        orderResponseDto.setOrderNumber(order.getOrderNumber());
//        orderResponseDto.setStatus(order.getStatus().toString());
//        orderResponseDto.setUserId(order.getUserId());
//        orderResponseDto.setSubTotal(String.valueOf(order.getSubTotal()));
//        orderResponseDto.setTotalAmount(String.valueOf(order.getTotalAmount()));
//        orderResponseDto.setItems(order.getItems().stream().map(OrderMapper::getOrderItemDto).toList());
//        return orderResponseDto;
//    }
//
//    public static OrderItemDto getOrderItemDto(OrderItem orderItem) {
//        OrderItemDto orderItemDto = new OrderItemDto();
//        orderItemDto.setPrice(orderItem.getPrice());
//        orderItemDto.setProductId(orderItem.getProductId());
//        orderItemDto.setQuantity(orderItem.getQuantity());
//        return orderItemDto;
//    }
//}
