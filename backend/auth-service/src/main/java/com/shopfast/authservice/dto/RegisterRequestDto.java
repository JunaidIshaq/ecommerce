package com.shopfast.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank
    @Email
    private String email;

    /**
     * Plain password over TLS. The old flow had the client AES-encrypt this with a
     * shared key; that added no security over TLS while forcing every client to hold
     * a server secret. Keycloak's password policy enforces strength on its side.
     */
    @NotBlank
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    private String password;

    private String firstName;

    private String lastName;
}
