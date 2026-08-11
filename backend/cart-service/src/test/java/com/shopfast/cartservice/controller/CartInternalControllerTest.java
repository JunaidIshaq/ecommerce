package com.shopfast.cartservice.controller;

import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartInternalController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CartInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CartService cartService;

    private CartItemDto sampleItem() {
        return CartItemDto.builder()
                .productId(UUID.randomUUID())
                .title("Widget")
                .price(new BigDecimal("9.99"))
                .quantity(2)
                .build();
    }

    @Test
    void getCart_byUserId() throws Exception {
        when(cartService.getCartItems(anyString())).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/cart/internal")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Widget"));

        verify(cartService).getCartItems(eq("user-1"));
    }

    @Test
    void getCart_byAnonId() throws Exception {
        when(cartService.getGuestCart(anyString())).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/cart/internal")
                        .header("X-Anon-Id", "anon-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Widget"));

        verify(cartService).getGuestCart(eq("anon-1"));
    }

    @Test
    void clearCart_byUserId() throws Exception {
        doNothing().when(cartService).clearCart(anyString());

        mockMvc.perform(delete("/api/v1/cart/internal")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isOk());

        verify(cartService).clearCart(eq("user-1"));
    }

    @Test
    void clearCart_byAnonId() throws Exception {
        doNothing().when(cartService).clearGuestCart(anyString());

        mockMvc.perform(delete("/api/v1/cart/internal")
                        .header("X-Anon-Id", "anon-1"))
                .andExpect(status().isOk());

        verify(cartService).clearGuestCart(eq("anon-1"));
    }

    @Test
    void getCart_missingHeaders_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/cart/internal"))
                .andExpect(status().isBadRequest());
    }
}
