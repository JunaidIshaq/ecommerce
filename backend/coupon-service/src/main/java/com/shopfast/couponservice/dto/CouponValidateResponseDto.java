package com.shopfast.couponservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponValidateResponseDto {

    private boolean valid;

    private double discount;

    private String reason;

    private String code;

}
