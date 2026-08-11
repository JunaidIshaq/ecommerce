package com.shopfast.authservice.controller;

import com.shopfast.authservice.service.CacheService;
import com.shopfast.authservice.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.authservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.doNothing;
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
    @MockBean private com.shopfast.authservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CacheService cacheService;
    @MockBean private TokenService tokenService;

    @Test
    void clearCaches_returnsOk() throws Exception {
        doNothing().when(cacheService).clearAllCache();

        mockMvc.perform(delete("/api/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache cleared"));
    }
}
