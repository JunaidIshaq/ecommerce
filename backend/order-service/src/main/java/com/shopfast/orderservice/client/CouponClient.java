package com.shopfast.orderservice.client;

import com.shopfast.common.events.CouponRedeemRequestDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.CouponValidateResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-service")
public interface CouponClient {

    @PostMapping("/api/v1/coupon/validate")
    CouponValidateResponseDto validate(@RequestBody CouponValidateRequestDto requestDto);

    @PostMapping("/api/v1/coupon/redeem")
    Void redeem(@RequestBody CouponRedeemRequestDto requestDto);

}
