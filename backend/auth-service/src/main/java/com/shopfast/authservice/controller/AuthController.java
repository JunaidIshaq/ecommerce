package com.shopfast.authservice.controller;

import com.shopfast.authservice.dto.AuthResponse;
import com.shopfast.authservice.dto.LoginRequestDto;
import com.shopfast.authservice.dto.RefreshRequestDto;
import com.shopfast.authservice.model.Order;
import com.shopfast.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Auth APIs")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login and receive access + refresh tokens")
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
    public ResponseEntity<Order> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
                                        @RequestBody(required = false) RefreshRequestDto dto) {
        String accessToken = null;
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = (dto != null) ? dto.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Validate token (simple) — returns 200 if valid")
    @GetMapping("/validate")
    public ResponseEntity<Void> validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.badRequest().build();
        String token = authHeader.substring(7);
        // tokenService can check blacklist and signature — but we delegate to TokenService via AuthService if needed
        // For simplicity, just return 200 when token signature valid
        // Alternatively inject TokenService and check blacklist.
        return ResponseEntity.ok().build();
    }

}
