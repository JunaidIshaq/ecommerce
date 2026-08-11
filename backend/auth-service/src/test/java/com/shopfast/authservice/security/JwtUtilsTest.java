package com.shopfast.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        setField("jwtSecret", "test-secret-key-at-least-256-bits-long-for-hmac-sha256!!");
        setField("accessTokenExpiresIn", 3600L);
        setField("refreshTokenExpiresIn", 86400L);
        jwtUtils.init();
    }

    private void setField(String name, Object value) throws Exception {
        Field f = JwtUtils.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(jwtUtils, value);
    }

    @Test
    void generateAndParseAccessTokenRoundTrips() {
        String token = jwtUtils.generateAccessToken("user-1", Map.of("role", "USER"));

        Claims claims = jwtUtils.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("role")).isEqualTo("USER");
    }

    @Test
    void generateRefreshTokenRoundTrips() {
        String token = jwtUtils.generateRefreshToken("user-42", Map.of());

        assertThat(jwtUtils.parseToken(token).getSubject()).isEqualTo("user-42");
    }

    @Test
    void isTokenValidAcceptsGoodToken() {
        String token = jwtUtils.generateAccessToken("u", Map.of());
        assertThat(jwtUtils.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValidRejectsTamperedToken() {
        String token = jwtUtils.generateAccessToken("u", Map.of());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtils.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValidRejectsGarbage() {
        assertThat(jwtUtils.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtUtils.isTokenValid("")).isFalse();
    }

    @Test
    void accessTokenExpirationIsConfigured() {
        assertThat(jwtUtils.getAccessTokenExpiresIn()).isEqualTo(3600L);
        assertThat(jwtUtils.getRefreshTokenExpiresIn()).isEqualTo(86400L);
    }
}
