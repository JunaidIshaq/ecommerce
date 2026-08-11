package com.shopfast.reviewservice.service;

import com.shopfast.reviewservice.dto.ReviewRequestDto;
import com.shopfast.reviewservice.dto.ReviewResponseDto;
import com.shopfast.reviewservice.model.RatingSummary;
import com.shopfast.reviewservice.model.Review;
import com.shopfast.reviewservice.repository.RatingSummaryRepository;
import com.shopfast.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RatingSummaryRepository ratingSummaryRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createOrUpdateReviewCreatesNewWhenAbsent() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(reviewRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId))
                .thenReturn(List.of(Review.builder().id(UUID.randomUUID()).productId(productId).userId(userId).rating(5).build()));

        ReviewRequestDto request = new ReviewRequestDto();
        request.setProductId(productId.toString());
        request.setRating(5);
        request.setTitle("Great");
        request.setComment("Nice");
        ReviewResponseDto dto = reviewService.createOrUpdateReview(userId, request);

        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getProductId()).isEqualTo(productId.toString());
        verify(ratingSummaryRepository).save(any(RatingSummary.class));
    }

    @Test
    void createOrUpdateReviewUpdatesExisting() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Review existing = Review.builder().id(UUID.randomUUID()).productId(productId).userId(userId).rating(2).build();
        when(reviewRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId))
                .thenReturn(List.of(existing));

        ReviewRequestDto request = new ReviewRequestDto();
        request.setProductId(productId.toString());
        request.setRating(4);
        request.setTitle("Better");
        request.setComment("Ok");
        ReviewResponseDto dto = reviewService.createOrUpdateReview(userId, request);

        assertThat(dto.getRating()).isEqualTo(4);
        verify(ratingSummaryRepository).save(any(RatingSummary.class));
    }

    @Test
    void getProductReviewsMapsEntities() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId))
                .thenReturn(List.of(Review.builder().id(UUID.randomUUID()).productId(productId).userId(userId).rating(3).build()));

        assertThat(reviewService.getProductReviews(productId)).hasSize(1);
    }

    @Test
    void getSummaryReturnsZeroWhenNone() {
        UUID productId = UUID.randomUUID();
        when(ratingSummaryRepository.findById(productId)).thenReturn(Optional.empty());

        var summary = reviewService.getSummary(productId);

        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getTotalReviews()).isZero();
    }

    @Test
    void deleteReviewRemovesAndUpdatesSummary() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Review review = Review.builder().id(UUID.randomUUID()).productId(productId).userId(userId).rating(5).build();
        when(reviewRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(userId, productId);

        verify(reviewRepository).delete(review);
        verify(ratingSummaryRepository).deleteById(productId);
    }

    @Test
    void deleteReviewIsNoopWhenMissing() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(reviewRepository.findByProductIdAndUserId(productId, userId)).thenReturn(Optional.empty());

        reviewService.deleteReview(userId, productId);

        verify(reviewRepository, never()).delete(any());
    }
}
