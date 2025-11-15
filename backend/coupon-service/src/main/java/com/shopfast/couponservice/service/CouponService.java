package com.shopfast.couponservice.service;

import com.shopfast.couponservice.dto.CouponCreateRequestDto;
import com.shopfast.couponservice.dto.CouponRedeemRequestDto;
import com.shopfast.couponservice.dto.CouponValidateRequestDto;
import com.shopfast.couponservice.dto.CouponValidateResponseDto;
import com.shopfast.couponservice.enums.CouponType;
import com.shopfast.couponservice.model.Coupon;
import com.shopfast.couponservice.repository.CouponRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public Coupon createCoupon(CouponCreateRequestDto requestDto) {
        String code = requestDto.getCode().trim().toUpperCase();
        if(couponRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        Coupon coupon = Coupon.builder()
                .code(code)
                .type(requestDto.getType())
                .value(requestDto.getValue())
                .minSubTotal(requestDto.getMinSubTotal())
                .applicableProductIds(requestDto.getApplicableProductIds())
                .maxUses(requestDto.getMaxUses())
                .maxUsesPerUser(requestDto.getMaxUsesPerUser())
                .usedCount(0)
                .startAt(requestDto.getStartAt())
                .endAt(requestDto.getEndAt())
                .createdAt(Instant.now())
                .build();
        return couponRepository.save(coupon);
    }

    /**
     * Validate coupon for user + subtotal + items
     */
    @Transactional
    public CouponValidateResponseDto validate(CouponValidateRequestDto requestDto) {
        Optional<Coupon> optionalCoupon = couponRepository.findByCode(requestDto.getCode().trim().toUpperCase());
        if (optionalCoupon.isEmpty()) {
            buildInvalid(requestDto.getCode(), "Coupon code not found !");
        }

        Coupon coupon = optionalCoupon.get();
        Instant now = Instant.now();
        if (coupon.getStartAt() != null && now.isBefore(coupon.getStartAt())) {
            return buildInvalid(coupon.getCode(), "Coupon code not started");
        }
        if (coupon.getEndAt() != null && now.isAfter(coupon.getEndAt())) {
            return buildInvalid(coupon.getCode(), "Coupon expired");
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            return buildInvalid(coupon.getCode(), "Coupon usage limit reached");
        }
        if (coupon.getMinSubTotal() != null && requestDto.getSubTotal() < coupon.getMinSubTotal()) {
            return buildInvalid(coupon.getCode(), "Minimum subtotal not met");
        }

        // product restrictions
        if (coupon.getApplicableProductIds() != null && requestDto.getItems() != null && !requestDto.getItems().isEmpty()) {
            Set<String> allowed = Arrays.stream(coupon.getApplicableProductIds().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            boolean anyMatch = requestDto.getItems().stream().anyMatch(i -> allowed.contains(i.getProductId()));
            if (!anyMatch) {
                return buildInvalid(coupon.getCode(), "Coupon not applicable to items in cart");
            }
        }

            // per-user checks: NOTE: we do not track per-user usage in this minimal service; you can extend with a Redemption table
            // For now, assume per-user limit not enforced or tracked. (Extend later)

            // compute discount
            double discount = calcDiscount(coupon, requestDto);
            if (discount <= 0) {
                return buildInvalid(coupon.getCode(), "No discount applicable");
            }

            return CouponValidateResponseDto.builder()
                    .valid(true)
                    .discount(round(discount))
                    .reason("OK")
                    .code(coupon.getCode())
                    .build();
    }

    private CouponValidateResponseDto buildInvalid(String code, String reason) {
        return CouponValidateResponseDto.builder()
                .valid(false)
                .discount(0.0)
                .reason(reason)
                .code(code)
                .build();
    }

    private double calcDiscount(Coupon coupon, CouponValidateRequestDto requestDto) {
        if(coupon.getType() == CouponType.FREE_SHIPPING) {
            // free shipping handled elsewhere; return some sentinel (0) - order service may interpret
            return 0.0;
        }
        if(coupon.getType() == CouponType.AMOUNT) {
            return Math.min(requestDto.getSubTotal(), Optional.ofNullable(coupon.getValue()).orElse(0.0));
        }
        if(coupon.getType() == CouponType.PERCENTAGE) {
            double percentage = Optional.ofNullable(coupon.getValue()).orElse(0.0);
            return requestDto.getSubTotal() * (percentage / 100.0);
        }
        return 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional
    public void redeem(CouponRedeemRequestDto requestDto) {
        Coupon coupon = couponRepository.findByCode(requestDto.getCode().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Coupon code not found "));
        if(coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new IllegalStateException("Coupon already exhausted");
        }
        coupon.setUsedCount(Optional.ofNullable(coupon.getUsedCount()).orElse(0) + 1);
        couponRepository.save(coupon);
        // TODO : persist user redemption record to enforce per-user limits
    }

    /**
     * Admin: Get coupon by code
     */
    public Optional<Coupon> findByCode(String code) {
        return couponRepository.findByCode(code.trim().toUpperCase());
    }



}
