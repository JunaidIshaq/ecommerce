package com.shopfast.common.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponRedeemRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private String userId;

}
