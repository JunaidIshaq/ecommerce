package com.shopfast.reviewservice.controller;

import com.shopfast.reviewservice.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.reviewservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CacheController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CacheControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.reviewservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CacheService cacheService;

    @Test
    void clearCaches_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache cleared"));

        verify(cacheService).clearAllCache();
    }
}
