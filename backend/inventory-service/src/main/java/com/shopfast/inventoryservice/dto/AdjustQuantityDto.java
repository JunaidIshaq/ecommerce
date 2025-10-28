package com.shopfast.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdjustQuantityDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(1)
    private int quantityChange;

}
