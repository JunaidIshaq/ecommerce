package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("sub_total")
    private String subTotal;

    @JsonProperty("discount")
    private String discount;

    @JsonProperty("total_amount")
    private String totalAmount;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public static AdminOrderDto from(Order order) {
        return AdminOrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .subTotal(order.getSubTotal().toString())
                .discount(order.getDiscount().toString())
                .totalAmount(order.getTotalAmount().toString())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
