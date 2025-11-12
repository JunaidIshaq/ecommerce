package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.AnyKeyJavaType;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("sub_total")
    private String subTotal;

    @JsonProperty("discount")
    private String discount;

    @JsonProperty("total_amount")
    private String totalAmount;

    @JsonProperty("order_status")
    private OrderStatus orderStatus;
    
    @JsonProperty("items")
    private List<OrderItemDto> items;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .orderNumber(order.getOrderNumber())
                .subTotal(order.getSubTotal().toString())
                .discount(order.getDiscount().toString())
                .totalAmount(order.getTotalAmount().toString())
                .status(order.getStatus().toString())
                .build();
    }

}