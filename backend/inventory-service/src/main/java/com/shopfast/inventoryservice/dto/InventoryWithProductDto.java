package com.shopfast.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.common.dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryWithProductDto implements Serializable {

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

    @JsonProperty("product")
    private ProductDto product;
}
