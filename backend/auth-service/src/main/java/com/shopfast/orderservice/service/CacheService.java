package com.shopfast.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void clearAllCache(){
        cacheManager.getCacheNames().forEach(cacheName -> {
            cacheManager.getCache(cacheName).clear();
            log.info("Cache {} has been cleared", cacheName);
        });
    }
}
