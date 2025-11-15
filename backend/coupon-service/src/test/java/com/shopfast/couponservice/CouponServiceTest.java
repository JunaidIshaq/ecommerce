package com.shopfast.couponservice;

import com.shopfast.couponservice.dto.CouponLineItemDto;
import com.shopfast.couponservice.dto.CouponValidateRequestDto;
import com.shopfast.couponservice.enums.CouponType;
import com.shopfast.couponservice.model.Coupon;
import com.shopfast.couponservice.repository.CouponRepository;
import com.shopfast.couponservice.service.CouponService;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

public class CouponServiceTest {

    private CouponRepository couponRepository = Mockito.mock(CouponRepository.class);
    private CouponService couponService;

    @BeforeEach
    void setup() {
        couponService = new CouponService(couponRepository);
    }

    @Test
    public void validatePercentageCoupon() {
        Coupon coupon = Coupon.builder()
                .code("WELCOME10")
                .type(CouponType.PERCENTAGE)
                .value(10.0)
                .minSubTotal(50.0)
                .startAt(Instant.now().minusSeconds(60))
                .endAt(Instant.now().plusSeconds(3600))
                .build();

        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidateRequestDto requestDto = new CouponValidateRequestDto();
        requestDto.setCode("WELCOME10");
        requestDto.setUserId("u1");
        requestDto.setSubTotal(200.0);
        requestDto.setItems(List.of(new CouponLineItemDto(){{
            setProductId("p1"); setPrice(100.0); setQuantity(2);
        }}));

        var result = couponService.validate(requestDto);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscount()).isEqualTo(20.0);
    }
}
