package com.shopfast.reviewservice.controller;

import com.shopfast.reviewservice.dto.RatingSummaryResponseDto;
import com.shopfast.reviewservice.service.ReviewService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewInternalController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class ReviewInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.reviewservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ReviewService reviewService;

    @Test
    void internalSummary_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        RatingSummaryResponseDto response = RatingSummaryResponseDto.builder()
                .averageRating(3.5)
                .totalReviews(2)
                .build();
        when(reviewService.getSummary(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/review/internal/summary/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(2));
    }
}
