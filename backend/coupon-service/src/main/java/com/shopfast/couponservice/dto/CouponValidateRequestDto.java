package com.shopfast.couponservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidateRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private String userId;

    @NotNull
    private Double subTotal;

    private List<CouponLineItemDto> Items;

}
