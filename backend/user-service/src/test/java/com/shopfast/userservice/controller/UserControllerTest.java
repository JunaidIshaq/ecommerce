package com.shopfast.userservice.controller;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.userservice.dto.RegisterRequestDto;
import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.service.UserService;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken("user@e.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void register_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("new@e.com").firstName("F").lastName("L").build();
        when(userService.registerNewUser(org.mockito.ArgumentMatchers.any(RegisterRequestDto.class))).thenReturn(user);

        RegisterRequestDto req = RegisterRequestDto.builder()
                .email("new@e.com").password("password123").firstName("F").lastName("L").build();

        mockMvc.perform(post("/api/v1/user")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@e.com"));
    }

    @Test
    void register_invalid_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("password", "short", "email", "not-an-email"));

        mockMvc.perform(post("/api/v1/user")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_returnsOk() throws Exception {
        UserDto dto = UserDto.builder().id(UUID.randomUUID()).email("a@e.com").build();
        PagedResponse<UserDto> page = new PagedResponse<>(List.of(dto), 1, 1, 1, 10);
        when(userService.getAllUsers(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("a@e.com"));
    }

    @Test
    void me_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("user@e.com").firstName("F").lastName("L").build();
        when(userService.findByEmail("user@e.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/user/me").with(userPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@e.com"));
    }

    private static RequestPostProcessor userPrincipal() {
        return request -> {
            var a = new UsernamePasswordAuthenticationToken("user@e.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            request.setUserPrincipal(a);
            return request;
        };
    }

    @Test
    void getById_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("a@e.com").firstName("F").lastName("L").build();
        when(userService.getById(id)).thenReturn(user);

        mockMvc.perform(get("/api/v1/user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@e.com"));
    }

    @Test
    void updateStatus_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("a@e.com").firstName("F").lastName("L").status(UserStatus.ACTIVE).build();
        when(userService.updateStatus(id, UserStatus.ACTIVE)).thenReturn(user);

        mockMvc.perform(patch("/api/v1/user/{id}/status", id).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void findByEmail_found_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("a@e.com").firstName("F").lastName("L").build();
        when(userService.findByEmail("a@e.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/user/internal/email").param("email", "a@e.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@e.com"));
    }

    @Test
    void findByEmail_notFound_returnsNotFound() throws Exception {
        when(userService.findByEmail("missing@e.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/user/internal/email").param("email", "missing@e.com"))
                .andExpect(status().isNotFound());
    }
}
