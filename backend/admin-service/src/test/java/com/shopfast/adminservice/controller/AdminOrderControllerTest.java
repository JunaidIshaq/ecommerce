package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.OrderAdminClient;
import com.shopfast.adminservice.service.AdminOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminOrderController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrderService service;

    @MockBean
    private OrderAdminClient orderAdminClient;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    @Test
    void getAllOrders_returnsOk() throws Exception {
        when(orderAdminClient.getAllOrders("admin", 0, 10, "PENDING"))
                .thenReturn(List.of("order1"));

        mockMvc.perform(get("/api/v1/admin/orders/pageNumber/0/pageSize/10")
                        .header("userId", "admin")
                        .param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderById_returnsOk() throws Exception {
        when(service.getOrderById("admin", "123"))
                .thenReturn(List.of("order-detail"));

        mockMvc.perform(get("/api/v1/admin/order/123")
                        .header("userId", "admin"))
                .andExpect(status().isOk());
    }
}
