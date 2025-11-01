package com.shopfast.userservice.events;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisProcessedEventStore {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "processed_event:";
    private static final Duration TTL = Duration.ofDays(7);

    public RedisProcessedEventStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String eventId) {
        return PREFIX + eventId;
    }

    public boolean markIfNotProcessed(String eventId) {
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key(eventId), "1", TTL);
        return Boolean.TRUE.equals(wasSet);
    }
}