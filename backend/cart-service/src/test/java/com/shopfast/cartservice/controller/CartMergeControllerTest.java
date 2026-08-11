package com.shopfast.cartservice.controller;

import com.shopfast.cartservice.dto.GuestMergeRequestDto;
import com.shopfast.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartMergeController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CartMergeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CartService cartService;

    private static final String USER_ID = "user-123";

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken(USER_ID, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    // Populates the controller's plain `Authentication` method argument (the
    // security filter chain is excluded, so the principal must be set on the
    // request itself rather than relying on SecurityContextHolder alone).
    private static RequestPostProcessor withUser() {
        return request -> {
            var auth = new UsernamePasswordAuthenticationToken(USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            request.setUserPrincipal(auth);
            return request;
        };
    }

    private GuestMergeRequestDto validMerge(String anonId) {
        GuestMergeRequestDto dto = new GuestMergeRequestDto();
        dto.setAnonId(anonId);
        return dto;
    }

    @Test
    void merge() throws Exception {
        doNothing().when(cartService).mergeGuestIntoUser(anyString(), anyString());

        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(withUser())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validMerge("anon-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).mergeGuestIntoUser(eq("anon-1"), eq(USER_ID));
    }

    @Test
    void merge_invalidBody_returns400() throws Exception {
        // anonId is @NotBlank -> blank value triggers validation failure
        String body = objectMapper.writeValueAsString(new GuestMergeRequestDto());

        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(withUser())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
