package com.shopfast.couponservice.service;

import com.shopfast.couponservice.dto.RatingSummaryResponseDto;
import com.shopfast.couponservice.dto.ReviewRequestDto;
import com.shopfast.couponservice.dto.ReviewResponseDto;
import com.shopfast.couponservice.model.RatingSummary;
import com.shopfast.couponservice.model.Review;
import com.shopfast.couponservice.repository.RatingSummaryRepository;
import com.shopfast.couponservice.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    private final RatingSummaryRepository ratingSummaryRepository;

    public ReviewService(ReviewRepository reviewRepository, RatingSummaryRepository ratingSummaryRepository) {
        this.reviewRepository = reviewRepository;
        this.ratingSummaryRepository = ratingSummaryRepository;
    }

    @Transactional
    public ReviewResponseDto createOrUpdateReview(UUID userId, ReviewRequestDto reviewRequestDto) {

        UUID productId = UUID.fromString(reviewRequestDto.getProductId());
        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElse(new Review());

        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(reviewRequestDto.getRating());
        review.setTitle(reviewRequestDto.getTitle());
        review.setComment(reviewRequestDto.getComment());
        review.setUpdatedAt(Instant.now());

        if(review.getCreatedAt() == null) {
            review.setCreatedAt(Instant.now());
        }

        reviewRepository.save(review);

        updateRatingSummary(productId);

        return toDto(review);
    }

    public List<ReviewResponseDto> getProductReviews(UUID productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public RatingSummaryResponseDto getSummary(UUID productId) {
        var summary = ratingSummaryRepository.findById(productId)
                .orElse(new RatingSummary(productId, 0.0, 0));
        return RatingSummaryResponseDto.builder()
                .averageRating(summary.getAverageRating())
                .totalReviews(summary.getTotalReviews())
                .build();
    }

    @Transactional
    public void deleteReview(UUID userId, UUID productId) {
        reviewRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(review -> {
                    reviewRepository.delete(review);
                    updateRatingSummary(productId);
                });
    }

    private void updateRatingSummary(UUID productId) {
        var list = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        if(list.isEmpty()) {
            ratingSummaryRepository.deleteById(productId);
            return;
        }
        double averageRating = list.stream().mapToInt(Review::getRating).average().orElse(0);
        long count = list.size();
        ratingSummaryRepository.save(RatingSummary.builder()
                .productId(productId)
                .averageRating(averageRating)
                .totalReviews(count)
                .build());
    }

    private ReviewResponseDto toDto(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId().toString())
                .productId(review.getProductId().toString())
                .userId(review.getUserId().toString())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .createdAt(String.valueOf(review.getCreatedAt()))
                .build();
    }


}
