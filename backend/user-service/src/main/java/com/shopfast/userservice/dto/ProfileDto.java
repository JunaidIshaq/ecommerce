package com.shopfast.userservice.dto;

import com.shopfast.userservice.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * What the signed-in user is allowed to see about themselves.
 *
 * <p>Deliberately separate from {@link UserDto}, which carries {@code password} - the
 * BCrypt hash - and would hand it to the browser on every profile load. A hash is not
 * a public value: it is offline-crackable, so leaking it converts "attacker read one
 * API response" into "attacker can try passwords forever at their own pace". This
 * record has no field to put it in, so the mistake cannot be reintroduced by editing
 * a mapper.
 *
 * @param fullName convenience for the UI header; the parts are also sent separately
 *                 so an edit form can bind to them without splitting a string.
 */
public record ProfileDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String country,
        String role,
        String status,
        Instant memberSince
) {

    public static ProfileDto from(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();

        return new ProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                (first + " " + last).trim(),
                user.getPhone(),
                user.getCountry(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getCreatedAt());
    }
}
