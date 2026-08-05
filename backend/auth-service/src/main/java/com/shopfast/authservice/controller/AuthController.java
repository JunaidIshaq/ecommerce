package com.shopfast.authservice.controller;

import com.shopfast.authservice.dto.AuthResponse;
import com.shopfast.authservice.dto.LoginRequestDto;
import com.shopfast.authservice.dto.RefreshRequestDto;
import com.shopfast.authservice.dto.RegisterRequestDto;
import com.shopfast.authservice.service.AuthService;
import com.shopfast.authservice.service.KeycloakAuthFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "Auth APIs")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final KeycloakAuthFacade keycloakAuthFacade;

    public AuthController(AuthService authService, KeycloakAuthFacade keycloakAuthFacade) {
        this.authService = authService;
        this.keycloakAuthFacade = keycloakAuthFacade;
    }

    @Operation(summary = "Register a new account in Keycloak and create the ShopFast profile")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequestDto dto) {
        // Always 202 with the same body, whether or not the email was free: a 409 here
        // would let anyone enumerate registered addresses.
        return ResponseEntity.accepted().body(keycloakAuthFacade.register(dto).toClientResponse());
    }

    @Operation(summary = "Send a password-reset email via Keycloak")
    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email is required"));
        }
        keycloakAuthFacade.requestPasswordReset(email);
        return ResponseEntity.accepted()
                .body(Map.of("message", "If that account exists, a reset email has been sent."));
    }

    @Operation(summary = "Sign out of every device by ending all Keycloak sessions")
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutEverywhere(
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        keycloakAuthFacade.logoutEverywhere(jwt.getSubject());
        return ResponseEntity.ok(Map.of("message", "All sessions ended"));
    }

    /**
     * @deprecated Tokens now come from Keycloak via Authorization Code + PKCE.
     * Kept only until the last legacy client is retired.
     */
    @Deprecated(forRemoval = true)
    @Operation(summary = "[Deprecated] Legacy login - use Keycloak Authorization Code + PKCE")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDto dto) {
        AuthResponse authResponse = authService.login(dto);
        return ResponseEntity.ok(authResponse);
    }


    @Operation(summary = "Refresh tokens using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequestDto dto) {
        AuthResponse authResponse = authService.refreshToken(dto.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }


    @Operation(summary = "Logout — revoke refresh token and blacklist access")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
                                        @RequestBody(required = false) RefreshRequestDto dto) {
        String accessToken = null;
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = (dto != null) ? dto.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully !"));
    }

    @Operation(summary = "Validate token (simple) — returns 200 if valid")
    @GetMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.badRequest().build();
        String token = authHeader.substring(7).trim();
        return authService.validate(token);
    }

}
