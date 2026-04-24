package com.shopfast.reviewservice.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RatingSummaryResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private double averageRating;

    private long totalReviews;

}
