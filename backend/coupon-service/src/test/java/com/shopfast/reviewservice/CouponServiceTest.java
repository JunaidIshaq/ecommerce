package com.shopfast.reviewservice.service;

import com.shopfast.common.events.CouponLineItemDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.CouponValidateResponseDto;
import com.shopfast.reviewservice.enums.CouponType;
import com.shopfast.reviewservice.model.Coupon;
import com.shopfast.reviewservice.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon sampleCoupon(CouponType type, double value) {
        return Coupon.builder()
                .code("SAVE10")
                .type(type)
                .value(value)
                .minSubTotal(50.0)
                .usedCount(0)
                .startAt(Instant.now().minusSeconds(3600))
                .endAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void createCouponNormalizesCodeToUppercaseAndSaves() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        com.shopfast.reviewservice.dto.CouponCreateRequestDto request =
                new com.shopfast.reviewservice.dto.CouponCreateRequestDto();
        request.setCode(" save10 ");
        request.setType(CouponType.PERCENTAGE);
        request.setValue(10.0);

        Coupon created = couponService.createCoupon(request);

        assertThat(created.getCode()).isEqualTo("SAVE10");
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void createCouponRejectsDuplicateCode() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        com.shopfast.reviewservice.dto.CouponCreateRequestDto request =
                new com.shopfast.reviewservice.dto.CouponCreateRequestDto();
        request.setCode("SAVE10");
        request.setType(CouponType.PERCENTAGE);
        request.setValue(10.0);

        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void validateReturnsValidForPercentageCoupon() {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(sampleCoupon(CouponType.PERCENTAGE, 10.0)));

        CouponValidateResponseDto response = couponService.validate(
                CouponValidateRequestDto.builder().code("save10").subTotal(100.0).build());

        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscount()).isEqualTo(10.0);
    }

    @Test
    void validateReturnsInvalidWhenCodeNotFound() {
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        CouponValidateResponseDto response = couponService.validate(
                CouponValidateRequestDto.builder().code("NOPE").subTotal(100.0).build());

        assertThat(response.isValid()).isFalse();
        assertThat(response.getReason()).contains("not found");
    }

    @Test
    void validateReturnsInvalidWhenBelowMinSubtotal() {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(sampleCoupon(CouponType.PERCENTAGE, 10.0)));

        CouponValidateResponseDto response = couponService.validate(
                CouponValidateRequestDto.builder().code("SAVE10").subTotal(10.0).build());

        assertThat(response.isValid()).isFalse();
        assertThat(response.getReason()).contains("Minimum subtotal");
    }

    @Test
    void validateReturnsInvalidWhenExpired() {
        Coupon expired = Coupon.builder()
                .code("OLD")
                .type(CouponType.AMOUNT)
                .value(5.0)
                .minSubTotal(0.0)
                .usedCount(0)
                .startAt(Instant.now().minusSeconds(7200))
                .endAt(Instant.now().minusSeconds(3600))
                .build();
        when(couponRepository.findByCode("OLD")).thenReturn(Optional.of(expired));

        CouponValidateResponseDto response = couponService.validate(
                CouponValidateRequestDto.builder().code("OLD").subTotal(100.0).build());

        assertThat(response.isValid()).isFalse();
        assertThat(response.getReason()).contains("expired");
    }

    @Test
    void validateAmountCouponCapsAtSubtotal() {
        when(couponRepository.findByCode("BIG")).thenReturn(Optional.of(
                Coupon.builder().code("BIG").type(CouponType.AMOUNT).value(80.0).minSubTotal(0.0)
                        .usedCount(0).build()));

        CouponValidateResponseDto response = couponService.validate(
                CouponValidateRequestDto.builder().code("BIG").subTotal(50.0).build());

        assertThat(response.getDiscount()).isEqualTo(50.0);
    }

    @Test
    void validateEnforcesApplicableProducts() {
        Coupon coupon = Coupon.builder().code("SPEC")
                .type(CouponType.PERCENTAGE).value(10.0).minSubTotal(0.0).usedCount(0)
                .applicableProductIds("aaa,bbb").build();
        when(couponRepository.findByCode("SPEC")).thenReturn(Optional.of(coupon));

        CouponLineItemDto lineItem = new CouponLineItemDto();
        lineItem.setProductId("zzz");
        CouponValidateRequestDto request = CouponValidateRequestDto.builder()
                .code("SPEC").subTotal(100.0)
                .Items(List.of(lineItem))
                .build();

        assertThat(couponService.validate(request).isValid()).isFalse();
    }

    @Test
    void redeemIncrementsUsedCount() {
        Coupon coupon = Coupon.builder().code("SAVE10").type(CouponType.AMOUNT).value(5.0)
                .usedCount(2).maxUses(10).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        com.shopfast.common.events.CouponRedeemRequestDto req = new com.shopfast.common.events.CouponRedeemRequestDto();
        req.setCode("SAVE10");
        couponService.redeem(req);

        assertThat(coupon.getUsedCount()).isEqualTo(3);
        verify(couponRepository).save(coupon);
    }

    @Test
    void redeemThrowsWhenExhausted() {
        Coupon coupon = Coupon.builder().code("SAVE10").type(CouponType.AMOUNT).value(5.0)
                .usedCount(10).maxUses(10).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        com.shopfast.common.events.CouponRedeemRequestDto req = new com.shopfast.common.events.CouponRedeemRequestDto();
        req.setCode("SAVE10");
        assertThatThrownBy(() -> couponService.redeem(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exhausted");
        verify(couponRepository, never()).save(any());
    }
}
