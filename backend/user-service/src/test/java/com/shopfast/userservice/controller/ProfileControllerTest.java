package com.shopfast.userservice.controller;

import com.shopfast.userservice.dto.ProfileDto;
import com.shopfast.userservice.dto.UpdateProfileRequest;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.service.ProfileService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {


    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ProfileService profileService;

    private Jwt jwt;

    @BeforeEach
    void auth() {
        jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "sub-123").build();
        var a = new UsernamePasswordAuthenticationToken(jwt, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void getProfile_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("u@e.com").firstName("A").lastName("B").build();
        when(profileService.getOrCreate(jwt)).thenReturn(user);

        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@e.com"))
                .andExpect(jsonPath("$.firstName").value("A"));
    }

    @Test
    void updateProfile_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("u@e.com").firstName("A").lastName("B").build();
        UpdateProfileRequest req = new UpdateProfileRequest("New", "Name", "+12345", "US");
        when(profileService.update(jwt, req)).thenReturn(user);

        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@e.com"));
    }
}
