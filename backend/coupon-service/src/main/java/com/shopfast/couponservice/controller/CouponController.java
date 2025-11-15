package com.shopfast.couponservice.controller;

import com.shopfast.couponservice.dto.CouponCreateRequestDto;
import com.shopfast.couponservice.dto.CouponRedeemRequestDto;
import com.shopfast.couponservice.dto.CouponValidateRequestDto;
import com.shopfast.couponservice.dto.CouponValidateResponseDto;
import com.shopfast.couponservice.model.Coupon;
import com.shopfast.couponservice.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupon", description = "Coupon APIs")
@RestController
@RequestMapping("/api/v1/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Admin only - create a coupon
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody CouponCreateRequestDto requestDto) {
        Coupon coupon = couponService.createCoupon(requestDto);
        return ResponseEntity.ok().body(coupon);
    }

    /**
     * Validate coupon (called by order service during checkout)
     */
    @PostMapping("/validate")
    public ResponseEntity<CouponValidateResponseDto> validate(@Valid @RequestBody CouponValidateRequestDto requestDto) {
        return ResponseEntity.ok(couponService.validate(requestDto));
    }

    /**
     * Redeem Coupon (call after order confirmed)
     */
    @PostMapping("/redeem")
    public ResponseEntity<Void> redeem(@Valid @RequestBody CouponRedeemRequestDto requestDto) {
        couponService.redeem(requestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Coupon> getByCode(@PathVariable String code) {
        return couponService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
