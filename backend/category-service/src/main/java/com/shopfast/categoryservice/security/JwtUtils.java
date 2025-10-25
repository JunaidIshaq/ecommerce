package com.shopfast.categoryservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public Claims extractClaimsJWT(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException ex) {
            log.error("Invalid JWT token : {}", ex.getMessage());
            throw new JwtException("Invalid JWT token");
        }
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
