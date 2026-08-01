package com.shopfast.authservice.service;

import com.shopfast.authservice.client.UserClient;
import com.shopfast.authservice.dto.AuthResponse;
import com.shopfast.authservice.dto.LoginRequestDto;
import com.shopfast.authservice.dto.UserInternalDto;
import com.shopfast.authservice.exception.InvalidCredentialsException;
import com.shopfast.common.utils.PasswordEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserClient userClient;

    private final TokenService tokenService;

    private final SecretKey encryptionKey;

    public AuthService(UserClient userClient, TokenService tokenService,
                       @Value("${app.password.encryption.key}") String base64EncryptionKey) {
        this.userClient = userClient;
        this.tokenService = tokenService;
        this.encryptionKey = PasswordEncryptionUtil.fromBase64Key(base64EncryptionKey);
    }

    public AuthResponse login(LoginRequestDto request) {
        try {
            UserInternalDto userDto = userClient.findByEmail(request.getEmail());
            if (userDto == null) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            if (userDto.getStatus() == null || !"ACTIVE".equals(userDto.getStatus().name())) {
                throw new IllegalArgumentException("User not active !");
            }

            // Decrypt the incoming encrypted password before BCrypt comparison
            String decryptedPassword = PasswordEncryptionUtil.decrypt(request.getPassword(), encryptionKey);

            boolean match = BCrypt.checkpw(decryptedPassword, userDto.getPassword());
            if (!match) {
                throw new InvalidCredentialsException("Invalid password");
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
        } catch (InvalidCredentialsException e) {
            throw e; // Let GlobalExceptionHandler return 401 with "Invalid password"
        } catch (Exception e) {
            throw new RuntimeException("User not exists with this email : " + request.getEmail() );
        }
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
        tokenService.revokeRefreshToken(refreshToken);

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
