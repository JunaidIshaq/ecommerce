package com.shopfast.authservice.service;

import com.shopfast.authservice.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class TokenService {

    public final JwtUtils jwtUtils;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public TokenService(JwtUtils jwtUtils, StringRedisTemplate stringRedisTemplate) {
        this.jwtUtils = jwtUtils;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String createAccessToken(String userId, Map<String, Object> claims) {
        return jwtUtils.generateAccessToken(userId, claims);
    }

    public String createRefreshToken(String userId, Map<String, Object> claims) {
        String token = jwtUtils.generateRefreshToken(userId, claims);
        // Store in redis with TTL = refresh expiry
        String key = REFRESH_PREFIX + extractTokenId(token);
        stringRedisTemplate.opsForValue().set(key, token, Duration.ofSeconds(jwtUtils.getRefreshTokenExpiresIn()));
        return token;
    }

    public boolean isRefreshTokenValid(String token) {
        String key = REFRESH_PREFIX + extractTokenId(token);
        String stored = stringRedisTemplate.opsForValue().get(key);
        return token.equals(stored) && jwtUtils.isTokenValid(token);
    }

    public void revokeRefreshToken(String token) {
        stringRedisTemplate.delete(REFRESH_PREFIX + extractTokenId(token));
    }

    public void blackListAccessToken(String token) {
        // keep blacklist entry until token expiry
        long ttl = jwtUtils.parseToken(token).getExpiration().toInstant().getEpochSecond() -
                jwtUtils.parseToken(token).getIssuedAt().toInstant().getEpochSecond();
        if (ttl <= 0) {
            ttl = jwtUtils.getAccessTokenExpiresIn();
            stringRedisTemplate.opsForValue().set(BLACKLIST_PREFIX + extractTokenId(token), "1", Duration.ofSeconds(ttl));
        }
    }

    public boolean isAccessTokenBlacklisted(String token) {
        return stringRedisTemplate.hasKey(BLACKLIST_PREFIX + extractTokenId(token));
    }

    private String extractTokenId(String token) {
        // Use UUID v4: create a deterministic id from token (hash) or random ID. For simplicity, store by token hash:
        return Integer.toHexString(token.hashCode());
    }
}
