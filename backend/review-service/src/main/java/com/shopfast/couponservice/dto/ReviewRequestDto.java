package com.shopfast.couponservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @NotNull
    private String productId;

    @Min(1)
    @Max(5)
    private Integer rating;

    private String title;

    @Size(max = 2000)
    private String comment;

}
