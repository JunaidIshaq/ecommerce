package com.shopfast.couponservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestMergeRequestDto {

    @NotBlank
    private String anonId;

}
