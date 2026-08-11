package com.shopfast.cartservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Test
    void clearAllCacheClearsEveryNamedCache() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("products"));
        when(cacheManager.getCache("products")).thenReturn(cache);

        new CacheService(cacheManager).clearAllCache();

        verify(cache).clear();
    }
}
