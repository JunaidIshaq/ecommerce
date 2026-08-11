package com.shopfast.cartservice.controller;

import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CartGuestController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CartGuestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CartService cartService;

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
    void addItem() throws Exception {
        doNothing().when(cartService).addGuest(anyString(), anyString(), anyInt());

        mockMvc.perform(post("/api/v1/cart/guest/items")
                        .param("anonId", "anon-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validItem())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).addGuest(eq("anon-1"), anyString(), anyInt());
    }

    @Test
    void addItem_invalidBody_returns400() throws Exception {
        // productId is @NotBlank -> missing it triggers validation failure
        String body = objectMapper.writeValueAsString(new CartItemRequestDto());

        mockMvc.perform(post("/api/v1/cart/guest/items")
                        .param("anonId", "anon-1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem() throws Exception {
        doNothing().when(cartService).updateGuest(anyString(), anyString(), anyInt());

        mockMvc.perform(put("/api/v1/cart/guest/items/{productId}", "prod-1")
                        .param("anonId", "anon-1")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).updateGuest(eq("anon-1"), eq("prod-1"), eq(3));
    }

    @Test
    void getCartItem() throws Exception {
        when(cartService.getGuestCart(anyString())).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/v1/cart/guest")
                        .param("anonId", "anon-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Widget"));

        verify(cartService).getGuestCart(eq("anon-1"));
    }

    @Test
    void removeItem() throws Exception {
        doNothing().when(cartService).removeGuestItem(anyString(), anyString());

        mockMvc.perform(delete("/api/v1/cart/guest/items/{productId}", "prod-1")
                        .param("anonId", "anon-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).removeGuestItem(eq("anon-1"), eq("prod-1"));
    }

    @Test
    void clear() throws Exception {
        doNothing().when(cartService).clearGuestCart(anyString());

        mockMvc.perform(delete("/api/v1/cart/guest")
                        .param("anonId", "anon-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).clearGuestCart(eq("anon-1"));
    }
}
