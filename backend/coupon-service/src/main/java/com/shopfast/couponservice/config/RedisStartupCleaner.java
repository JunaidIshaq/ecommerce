package com.shopfast.couponservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisStartupCleaner {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisStartupCleaner(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @PostConstruct
    public void clearOnStartup() {
        redisConnectionFactory.getConnection().serverCommands().flushAll();
        log.info("✅ Redis cache cleared on startup (local dev mode)");
    }
}
