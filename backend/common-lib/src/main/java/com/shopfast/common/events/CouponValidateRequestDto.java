package com.shopfast.common.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidateRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String code;

    @NotNull
    private String userId;

    @NotNull
    private Double subTotal;

    private List<CouponLineItemDto> Items;

}
