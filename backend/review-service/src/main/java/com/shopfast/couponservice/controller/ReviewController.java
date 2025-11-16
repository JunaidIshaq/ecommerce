package com.shopfast.couponservice.controller;

import com.shopfast.couponservice.dto.RatingSummaryResponseDto;
import com.shopfast.couponservice.dto.ReviewRequestDto;
import com.shopfast.couponservice.dto.ReviewResponseDto;
import com.shopfast.couponservice.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Review", description = "Review APIs")
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }


    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(@Valid @RequestBody ReviewRequestDto requestDto, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ReviewResponseDto review = reviewService.createOrUpdateReview(userId, requestDto);
        return ResponseEntity.ok().body(review);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(UUID.fromString(productId)));
    }

    @GetMapping("/summary/{productId}")
    public ResponseEntity<RatingSummaryResponseDto> getSummary(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getSummary(UUID.fromString(productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteReview(@PathVariable String productId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        reviewService.deleteReview(userId, UUID.fromString(productId));
        return ResponseEntity.ok().build();
    }


}
