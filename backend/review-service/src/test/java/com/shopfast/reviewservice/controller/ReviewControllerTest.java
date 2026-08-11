package com.shopfast.reviewservice.controller;

import com.shopfast.reviewservice.dto.RatingSummaryResponseDto;
import com.shopfast.reviewservice.dto.ReviewRequestDto;
import com.shopfast.reviewservice.dto.ReviewResponseDto;
import com.shopfast.reviewservice.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.reviewservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    private static final String USER_UUID = "00000000-0000-0000-0000-000000000001";
    private static final String PRODUCT_UUID = "00000000-0000-0000-0000-000000000002";

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.reviewservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReviewService reviewService;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken(USER_UUID, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void createReview_returnsOk() throws Exception {
        ReviewRequestDto request = new ReviewRequestDto();
        request.setProductId(PRODUCT_UUID);
        request.setRating(5);
        request.setTitle("Great");
        request.setComment("Loved it");

        ReviewResponseDto response = ReviewResponseDto.builder()
                .id("rev-1")
                .productId(PRODUCT_UUID)
                .userId(USER_UUID)
                .rating(5)
                .build();
        when(reviewService.createOrUpdateReview(any(UUID.class), any(ReviewRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/review")
                        .principal(new UsernamePasswordAuthenticationToken(USER_UUID, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void createReview_invalidBody_returnsBadRequest() throws Exception {
        ReviewRequestDto request = new ReviewRequestDto();
        request.setProductId(PRODUCT_UUID);
        request.setRating(10);

        mockMvc.perform(post("/api/v1/review")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductReviews_returnsOk() throws Exception {
        ReviewResponseDto response = ReviewResponseDto.builder()
                .id("rev-1")
                .productId(PRODUCT_UUID)
                .rating(4)
                .build();
        when(reviewService.getProductReviews(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/review/product/{productId}", PRODUCT_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    @Test
    void getSummary_returnsOk() throws Exception {
        RatingSummaryResponseDto response = RatingSummaryResponseDto.builder()
                .averageRating(4.5)
                .totalReviews(10)
                .build();
        when(reviewService.getSummary(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/review/summary/{productId}", PRODUCT_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5));
    }

    @Test
    void deleteReview_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/review/{productId}", PRODUCT_UUID)
                        .principal(new UsernamePasswordAuthenticationToken(USER_UUID, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")))))
                .andExpect(status().isOk());

        verify(reviewService).deleteReview(any(UUID.class), any(UUID.class));
    }
}
