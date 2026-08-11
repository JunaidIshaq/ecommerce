package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.UserAdminClient;
import com.shopfast.adminservice.service.AdminUserService;
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

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService service;
    @MockBean
    private UserAdminClient userAdminClient;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void getAllUsers_returnsOk() throws Exception {
        when(userAdminClient.getAllUsers("admin", 0, 10, "CUSTOMER"))
                .thenReturn(List.of("user1"));

        mockMvc.perform(get("/api/v1/admin/users/pageNumber/0/pageSize/10")
                        .header("userId", "admin")
                        .param("role", "CUSTOMER"))
                .andExpect(status().isOk());
    }

    @Test
    void blockUser_returnsOk() throws Exception {
        doNothing().when(service).blockUser(1L, "admin");

        mockMvc.perform(put("/api/v1/admin/1/block")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                .andExpect(status().isOk());
    }
}
