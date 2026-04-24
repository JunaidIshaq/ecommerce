package com.shopfast.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class InventoryRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @JsonProperty("productId")
    private UUID productId;

    @Min(0)
    private int availableQuantity;

}
