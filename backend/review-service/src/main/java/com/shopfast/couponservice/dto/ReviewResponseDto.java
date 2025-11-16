package com.shopfast.couponservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponseDto {

    private String id;

    private String productId;

    private String userId;

    private Integer rating;

    private String title;

    private String comment;

    private String createdAt;

    private String updatedAt;

}
