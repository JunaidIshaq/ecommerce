package com.shopfast.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String refreshToken;

}
