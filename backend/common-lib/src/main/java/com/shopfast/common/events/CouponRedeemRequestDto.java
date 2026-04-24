package com.shopfast.common.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CouponRedeemRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String code;

    @NotNull
    private String userId;

}
