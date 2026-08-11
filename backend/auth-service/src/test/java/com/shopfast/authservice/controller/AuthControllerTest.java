package com.shopfast.authservice.controller;

import com.shopfast.authservice.dto.AuthResponse;
import com.shopfast.authservice.dto.LoginRequestDto;
import com.shopfast.authservice.dto.RefreshRequestDto;
import com.shopfast.authservice.dto.RegisterRequestDto;
import com.shopfast.authservice.service.AuthService;
import com.shopfast.authservice.service.KeycloakAuthFacade;
import com.shopfast.authservice.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.authservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {


    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.authservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private KeycloakAuthFacade keycloakAuthFacade;
    @MockBean private TokenService tokenService;

    private Jwt jwt;

    @BeforeEach
    void auth() {
        jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "sub-123").build();
        var a = new UsernamePasswordAuthenticationToken(jwt, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void register_returnsAccepted() throws Exception {
        when(keycloakAuthFacade.register(any(RegisterRequestDto.class)))
                .thenReturn(new KeycloakAuthFacade.RegistrationResult("k", "u", true));

        RegisterRequestDto req = new RegisterRequestDto("a@b.com", "ValidPass1234", "F", "L");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    void register_invalid_returnsBadRequest() throws Exception {
        Map<String, Object> body = Map.of("email", "not-an-email", "password", "short");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPasswordReset_returnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", "a@b.com"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void logoutEverywhere_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_returnsOk() throws Exception {
        when(authService.login(any(LoginRequestDto.class)))
                .thenReturn(AuthResponse.builder().accessToken("tok").accessTokenExpiresIn(1).build());

        LoginRequestDto req = new LoginRequestDto();
        req.setEmail("a@b.com");
        req.setPassword("pw");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok"));
    }

    @Test
    void refresh_returnsOk() throws Exception {
        when(authService.refreshToken(anyString()))
                .thenReturn(AuthResponse.builder().accessToken("new-tok").accessTokenExpiresIn(1).build());

        RefreshRequestDto req = new RefreshRequestDto();
        req.setRefreshToken("refresh-tok");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-tok"));
    }

    @Test
    void logout_returnsOk() throws Exception {
        RefreshRequestDto req = new RefreshRequestDto();
        req.setRefreshToken("refresh-tok");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-tok")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void validate_returnsOk() throws Exception {
        when(authService.validate(anyString())).thenReturn(ResponseEntity.ok("Token valid"));

        mockMvc.perform(get("/api/v1/auth/validate").header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk());
    }
}
