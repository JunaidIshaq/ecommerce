package com.shopfast.couponservice.dto;

import com.shopfast.couponservice.enums.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.Instant;

@Data
public class CouponCreateRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private CouponType type;

    @PositiveOrZero
    private Double value;

    @PositiveOrZero
    private Double minSubTotal;

    private String applicableProductIds; // comma-separated

    @PositiveOrZero
    private Integer maxUses;

    @PositiveOrZero
    private Integer maxUsesPerUser;

    private Instant startAt;

    private Instant endAt;

}
