package com.shopfast.paymentservice.util;

import com.shopfast.paymentservice.dto.OrderItemDto;
import com.shopfast.paymentservice.dto.PaymentResponseDto;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.model.OrderItem;

public class OrderMapper {


//    public static PaymentResponseDto getOrderResponseDto(Payment payment) {
//        PaymentResponseDto paymentResponseDto = new PaymentResponseDto();
//        paymentResponseDto.setId(payment.getId());
//        paymentResponseDto.setUserId(payment.getUserId());
//        paymentResponseDto.setOrderId(payment.getOrderId());
//        paymentResponseDto.setStatus(payment.getStatus());
//        paymentResponseDto.setUserId(payment.getUserId());
//        paymentResponseDto.setAmount(String.valueOf(payment.getAmount()));
//        paymentResponseDto.setItems(payment.getItems().stream().map(OrderMapper::getOrderItemDto).toList());
//        return paymentResponseDto;
//    }
//
//    public static OrderItemDto getOrderItemDto(OrderItem orderItem) {
//        OrderItemDto orderItemDto = new OrderItemDto();
//        orderItemDto.setPrice(orderItem.getPrice());
//        orderItemDto.setProductId(orderItem.getProductId());
//        orderItemDto.setQuantity(orderItem.getQuantity());
//        return orderItemDto;
//    }
}
