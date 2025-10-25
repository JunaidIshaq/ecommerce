package com.shopfast.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InventoryRequestDto {

    @NotNull
    @JsonProperty("productId")
    private UUID productId;

    @Min(0)
    private int availableQuantity;

}
