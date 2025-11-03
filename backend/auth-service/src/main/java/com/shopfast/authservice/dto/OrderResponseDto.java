package com.shopfast.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
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

    @JsonProperty("total_amount")
    private String totalAmount;
    
    @JsonProperty("items")
    private List<OrderItemDto> items;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

}