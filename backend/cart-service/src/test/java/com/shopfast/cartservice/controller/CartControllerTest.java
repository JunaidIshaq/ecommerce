package com.shopfast.cartservice.controller;

import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
import com.shopfast.cartservice.service.CartService;
import com.shopfast.cartservice.web.CartIdentity;
import com.shopfast.cartservice.web.CartIdentityResolver;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CartService cartService;
    @MockBean private CartIdentityResolver identityResolver;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken("user", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    private CartItemRequestDto validItem() {
        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(UUID.randomUUID().toString());
        dto.setQuantity(2);
        return dto;
    }

    private CartItemDto sampleItem() {
        return CartItemDto.builder()
                .productId(UUID.randomUUID())
                .title("Widget")
                .price(new BigDecimal("9.99"))
                .quantity(2)
                .build();
    }

    @Test
    void addItem_user() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("user-1", false));
        doNothing().when(cartService).addUser(anyString(), anyString(), anyInt());

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validItem())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).addUser(anyString(), anyString(), anyInt());
    }

    @Test
    void addItem_guest() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("anon-1", true));
        doNothing().when(cartService).addGuest(anyString(), anyString(), anyInt());

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validItem())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).addGuest(anyString(), anyString(), anyInt());
    }

    @Test
    void updateItem_user() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("user-1", false));
        doNothing().when(cartService).updateUser(anyString(), anyString(), anyInt());

        mockMvc.perform(put("/api/v1/cart/items/{productId}", "prod-1")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).updateUser(anyString(), eq("prod-1"), eq(3));
    }

    @Test
    void getCartItems_user() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("user-1", false));
        when(cartService.getUserCart(anyString())).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Widget"));

        verify(cartService).getUserCart(anyString());
    }

    @Test
    void removeItem_user() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("user-1", false));
        doNothing().when(cartService).removeUserItem(anyString(), anyString());

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).removeUserItem(anyString(), eq("prod-1"));
    }

    @Test
    void clearCart_user() throws Exception {
        when(identityResolver.resolve(any(), any())).thenReturn(new CartIdentity("user-1", false));
        doNothing().when(cartService).clearUserCart(anyString());

        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).clearUserCart(anyString());
    }
}
