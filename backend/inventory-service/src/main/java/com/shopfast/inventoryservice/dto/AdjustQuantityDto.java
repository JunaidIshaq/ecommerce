package com.shopfast.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdjustQuantityDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(1)
    private int quantityChange;

}
