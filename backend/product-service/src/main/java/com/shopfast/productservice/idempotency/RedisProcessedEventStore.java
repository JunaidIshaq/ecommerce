package com.shopfast.productservice.idempotency;

import com.shopfast.productservice.events.ProcessedEventStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisProcessedEventStore implements ProcessedEventStore {

    private final StringRedisTemplate redisTemplate;

    public RedisProcessedEventStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private static final Duration TTL = Duration.ofDays(7); // keep processed IDs for 7 days

    private String key(String eventId) {
        return "processed_event:" + eventId;
    }

    @Override
    public boolean isProcessed(String eventId) {
        return redisTemplate.hasKey(key(eventId));
    }

    @Override
    public boolean markProcessed(String eventId) {
        // set if absent, return true if set (not previously present)
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key(eventId), "1", TTL);
        return Boolean.TRUE.equals(wasSet);
    }

}
