package com.shopfast.reviewservice.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ReviewResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String productId;

    private String userId;

    private Integer rating;

    private String title;

    private String comment;

    private String createdAt;

    private String updatedAt;

}
