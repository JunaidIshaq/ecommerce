package com.shopfast.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
