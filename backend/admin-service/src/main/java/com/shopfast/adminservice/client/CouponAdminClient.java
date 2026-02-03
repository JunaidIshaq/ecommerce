package com.shopfast.adminservice.client;

import com.shopfast.adminservice.dto.CouponDto;
import com.shopfast.adminservice.dto.CreateCouponRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-service", url= "coupon.service.url",path = "/internal/admin/coupons")
public interface CouponAdminClient {

    @PostMapping
    CouponDto createCoupon(@RequestBody CreateCouponRequest request);

    @PutMapping("/{id}/disable")
    void disableCoupon(@PathVariable Long id);

}

