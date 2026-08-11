package com.shopfast.reviewservice.controller;

import com.shopfast.reviewservice.dto.CouponCreateRequestDto;
import com.shopfast.reviewservice.enums.CouponType;
import com.shopfast.reviewservice.model.Coupon;
import com.shopfast.reviewservice.service.CouponService;
import com.shopfast.common.events.CouponRedeemRequestDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.CouponValidateResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.reviewservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CouponController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CouponControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.reviewservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CouponService couponService;

    @Test
    void createCoupon_returnsOk() throws Exception {
        CouponCreateRequestDto dto = new CouponCreateRequestDto();
        dto.setCode("SAVE10");
        dto.setType(CouponType.PERCENTAGE);
        dto.setValue(10.0);
        dto.setMinSubTotal(50.0);

        Coupon saved = Coupon.builder()
                .id(UUID.randomUUID())
                .code("SAVE10")
                .type(CouponType.PERCENTAGE)
                .value(10.0)
                .build();

        when(couponService.createCoupon(any(CouponCreateRequestDto.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/coupon")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"));
    }

    @Test
    void createCoupon_invalidBody_returnsBadRequest() throws Exception {
        CouponCreateRequestDto dto = new CouponCreateRequestDto();

        mockMvc.perform(post("/api/v1/coupon")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_returnsOk() throws Exception {
        CouponValidateRequestDto dto = CouponValidateRequestDto.builder()
                .code("SAVE10")
                .userId("user-1")
                .subTotal(100.0)
                .build();

        CouponValidateResponseDto resp = CouponValidateResponseDto.builder()
                .valid(true)
                .discount(10.0)
                .reason("OK")
                .code("SAVE10")
                .build();

        when(couponService.validate(any(CouponValidateRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/coupon/validate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void redeem_returnsOk() throws Exception {
        CouponRedeemRequestDto dto = new CouponRedeemRequestDto();
        dto.setCode("SAVE10");
        dto.setUserId("user-1");

        mockMvc.perform(post("/api/v1/coupon/redeem")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void getByCode_found_returnsOk() throws Exception {
        Coupon coupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("SAVE10")
                .type(CouponType.PERCENTAGE)
                .value(10.0)
                .build();

        when(couponService.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        mockMvc.perform(get("/api/v1/coupon/SAVE10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"));
    }

    @Test
    void getByCode_notFound_returnsNotFound() throws Exception {
        when(couponService.findByCode("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/coupon/NOPE"))
                .andExpect(status().isNotFound());
    }
}
