package com.shopfast.userservice.controller;

import com.shopfast.userservice.dto.ProfileDto;
import com.shopfast.userservice.dto.UpdateProfileRequest;
import com.shopfast.userservice.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own profile.
 *
 * <p>There is no {@code {id}} in these paths, and that is the point. An endpoint like
 * {@code GET /user/{id}} invites the caller to nominate whose data they want, and then
 * relies on every handler remembering to check that they may - the omission that made
 * order lookup readable by anyone. Here the subject comes from the signed token, so
 * "another user's profile" is not expressible in the URL.
 *
 * <p>Identity is read via {@link AuthenticationPrincipal} rather than the
 * {@code userId} header the gateway injects. The header is a compatibility shim for
 * older controllers; the token is the real evidence, and it is verified locally by
 * this service's resource-server chain.
 */
@Tag(name = "Profile", description = "Current user's profile")
@RestController
@RequestMapping("/api/v1/user/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get the signed-in user's profile")
    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ProfileDto.from(profileService.getOrCreate(jwt)));
    }

    @Operation(summary = "Update the signed-in user's profile")
    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody UpdateProfileRequest request) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ProfileDto.from(profileService.update(jwt, request)));
    }
}
