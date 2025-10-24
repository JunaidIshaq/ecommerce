package com.shopfast.productservice.controller;

import com.shopfast.productservice.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCaches(){
        cacheService.clearAllCache();
        return ResponseEntity.ok("Cache cleared");
    }
}
