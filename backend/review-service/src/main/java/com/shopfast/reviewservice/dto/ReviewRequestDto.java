package com.shopfast.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ReviewRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String productId;

    @Min(1)
    @Max(5)
    private Integer rating;

    private String title;

    @Size(max = 2000)
    private String comment;

}
