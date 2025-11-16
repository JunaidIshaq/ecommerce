package com.shopfast.couponservice.controller;

import com.shopfast.couponservice.dto.RatingSummaryResponseDto;
import com.shopfast.couponservice.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review/internal")
public class ReviewInternalController {

    private final ReviewService reviewService;

    public ReviewInternalController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/summary/{productId}")
    public RatingSummaryResponseDto internalSummary(@PathVariable UUID productId) {
        return reviewService.getSummary(productId);
    }

}
