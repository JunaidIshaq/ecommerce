package com.shopfast.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class InventoryResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("available_quantity")
    private int availableQuantity;

    @JsonProperty("reserved_quantity")
    private int reservedQuantity;

    @JsonProperty("sold_quantity")
    private int soldQuantity;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

}