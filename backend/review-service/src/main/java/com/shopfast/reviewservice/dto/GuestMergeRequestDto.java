package com.shopfast.reviewservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class GuestMergeRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String anonId;

}
