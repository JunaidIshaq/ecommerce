package com.shopfast.common.events;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CouponValidateResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean valid;

    private double discount;

    private String reason;

    private String code;

}
