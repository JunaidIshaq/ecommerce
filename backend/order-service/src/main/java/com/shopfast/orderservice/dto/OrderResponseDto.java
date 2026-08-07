package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import com.shopfast.orderservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("payment_status")
    private OrderStatus paymentStatus;

    @JsonProperty("items")
    private List<OrderItemDto> items;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * Returned once, at guest checkout, and never by {@link #from(Order)} - a token
     * echoed back on every read would leak from any endpoint that renders an order.
     * Omitted from the payload when null.
     */
    @JsonProperty("access_token")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String accessToken;

    public static OrderResponseDto from(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId() != null ? order.getId().toString() : null)
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().toString())
                .subTotal(order.getSubTotal().toString())
                .discount(order.getDiscount() != null ? order.getDiscount().toString() : "0")
                .totalAmount(order.getTotalAmount().toString())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .items(order.getItems() != null ? order.getItems().stream().map(i -> OrderItemDto.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build()).toList() : null)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
                .build();
    }

}
