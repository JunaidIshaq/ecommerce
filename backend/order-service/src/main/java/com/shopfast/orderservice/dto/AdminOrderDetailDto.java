package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDetailDto {

    private UUID id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("sub_total")
    private String subTotal;

    private String discount;

    @JsonProperty("total_amount")
    private String totalAmount;

    private String status;

    private List<AdminOrderItemDetailDto> items;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
