package com.shopfast.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The fields a user may change about themselves.
 *
 * <p>Note what is absent: {@code email}, {@code role}, {@code status} and {@code id}.
 * Binding those here would let anyone grant themselves {@code ROLE_ADMIN} or unblock
 * their own suspended account with an ordinary profile save - the classic mass
 * assignment bug. Email is excluded for a further reason: Keycloak owns it, and
 * changing it locally would only desynchronise the two.
 */
public record UpdateProfileRequest(

        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        // Deliberately permissive - phone formats vary by country and over-strict
        // validation rejects legitimate numbers. This only bounds length and
        // character set to keep obvious junk out of the column.
        @Size(max = 30, message = "Phone must be at most 30 characters")
        @Pattern(regexp = "^$|^[+0-9()\\-.\\s]{5,30}$", message = "Phone contains invalid characters")
        String phone,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String country
) {
}
