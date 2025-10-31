package com.shopfast.paymentservice.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisPaymentIdempotencyStore {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "payment:idempotency:";

    public RedisPaymentIdempotencyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // keep lock for 1 hour (adjust as required)
    private static final Duration TTL = Duration.ofHours(1);

    private String key(String orderId) {
        return PREFIX + orderId;
    }

    /**
     * Try to claim idempotency token for the given order.
     * Returns true if this call is the first (token set), false if already claimed.
     */
    public boolean tryClaim(String orderId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key(orderId), "1", TTL);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Optionally, you can clear the token (e.g., on manual rollback).
     */
    public void clear(String orderId) {
        redisTemplate.delete(key(orderId));
    }
}
