package com.shopfast.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RegisterRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private String firstName;

    private String lastName;

}
