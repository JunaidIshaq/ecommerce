package com.shopfast.reviewservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingSummaryResponseDto {

    private double averageRating;

    private long totalReviews;

}
