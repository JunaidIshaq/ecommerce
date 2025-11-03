package com.shopfast.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDto {

    @NotBlank
    private String refreshToken;

}
