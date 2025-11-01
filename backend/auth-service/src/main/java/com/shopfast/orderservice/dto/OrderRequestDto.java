package com.shopfast.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrderRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty
    private List<OrderItemDto> items;

    @NotNull
    @JsonProperty("user_id")
    private String userId;

}
