package com.shopfast.authservice.service;

import com.shopfast.authservice.client.UserClient;
import com.shopfast.authservice.dto.AuthResponse;
import com.shopfast.authservice.dto.LoginRequestDto;
import com.shopfast.authservice.dto.UserInternalDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserClient userClient;

    private final TokenService tokenService;

    public AuthService(UserClient userClient, TokenService tokenService) {
        this.userClient = userClient;
        this.tokenService = tokenService;
    }

    public AuthResponse login(LoginRequestDto request) {
        UserInternalDto userDto = userClient.findByEmail(request.getEmail());
        if(userDto == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if(userDto.getStatus() == null || !"ACTIVE".equals(userDto.getStatus().name())) {
            throw new IllegalArgumentException("User not active !");
        }

        boolean match = BCrypt.checkpw(request.getPassword(), userDto.getPassword());
        if(!match) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String userId = userDto.getId().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDto.getRole().name());
        claims.put("email", userDto.getEmail());

        String access = tokenService.createAccessToken(userId, claims);
        String refresh = tokenService.createRefreshToken(userId, Map.of("email", userDto.getEmail()));

        return AuthResponse.builder()
                .accessToken(access)
                .accessTokenExpiresIn(tokenService.jwtUtils.getAccessTokenExpiresIn())
                .refreshToken(refresh)
                .refreshTokenExpiresIn(tokenService.jwtUtils.getRefreshTokenExpiresIn())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if(!tokenService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token invalid");
        }
        var parsed = tokenService.jwtUtils.parseToken(refreshToken);
        String userId = parsed.getSubject();
        String email = parsed.get("email", String.class);

        Map<String, Object> claims = Map.of("email", email);
        String newAccess = tokenService.createAccessToken(userId, claims);
        String newRefresh = tokenService.createRefreshToken(userId, Map.of("email", email));
        // revoke old refresh
        tokenService.revokeRefreshToken(newRefresh);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .accessTokenExpiresIn(tokenService.jwtUtils.getAccessTokenExpiresIn())
                .refreshToken(newRefresh)
                .refreshTokenExpiresIn(tokenService.jwtUtils.getRefreshTokenExpiresIn())
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        if(accessToken !=null && !accessToken.isBlank()) {
            tokenService.blackListAccessToken(accessToken);
        }
        if(refreshToken !=null && !refreshToken.isBlank()) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }


    public ResponseEntity<String> validate(String token) {
        try {
            // Check if token is blacklisted
            if (tokenService.isAccessTokenBlacklisted(token)) {
                return ResponseEntity.status(401).body("Token has been revoked or blacklisted");
            }

            // Validate JWT signature and expiration
            boolean valid = tokenService.jwtUtils.isTokenValid(token);
            if(!valid) {
                return ResponseEntity.status(401).body("Invalid or expired token");
            }

            // Optional : Extract claims if you want to return token info
            var claims = tokenService.jwtUtils.parseToken(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Return success response
            return ResponseEntity.ok("Token valid for userId : " + userId + ", role : " + role);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token : " + e.getMessage());
        }
    }
}
