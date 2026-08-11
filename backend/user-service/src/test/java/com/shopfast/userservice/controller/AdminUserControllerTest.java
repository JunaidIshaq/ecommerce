package com.shopfast.userservice.controller;

import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserRepository userRepository;

    @Test
    void getUsersPage_returnsOk() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("a@e.com").firstName("F").lastName("L").build();
        Page<User> page = new PageImpl<>(List.of(user), Pageable.ofSize(10), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/user/internal/admin/users/pageNumber/1/pageSize/10")
                        .header("userId", "admin-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("a@e.com"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }
}
