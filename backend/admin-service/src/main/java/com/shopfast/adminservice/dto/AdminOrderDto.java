package com.shopfast.adminservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

}
