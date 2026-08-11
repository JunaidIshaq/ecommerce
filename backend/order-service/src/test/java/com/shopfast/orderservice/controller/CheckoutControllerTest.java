package com.shopfast.orderservice.controller;

import com.shopfast.orderservice.dto.CheckoutRequestDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.service.CheckoutService;
import com.shopfast.orderservice.web.OrderIdentity;
import com.shopfast.orderservice.web.OrderIdentityResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.orderservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CheckoutController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CheckoutControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.orderservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CheckoutService checkoutService;
    @MockBean private OrderIdentityResolver identityResolver;

    private Order baseOrder() {
        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .userId("user")
                .orderNumber("ORD-1")
                .status(OrderStatus.CREATED)
                .paymentStatus(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .subTotal(new BigDecimal("20.00"))
                .discount(new BigDecimal("0"))
                .totalAmount(new BigDecimal("20.00"))
                .items(List.of(item))
                .build();
    }

    @Test
    void checkout_authenticated_returnsOk() throws Exception {
        Order order = baseOrder();
        when(identityResolver.resolve(any(), any())).thenReturn(OrderIdentity.ofUser("user"));
        when(checkoutService.checkout(any(OrderIdentity.class), any())).thenReturn(order);

        CheckoutRequestDto request = new CheckoutRequestDto();
        request.setCouponCode("SAVE10");

        var principal = new UsernamePasswordAuthenticationToken("user", null, List.of());

        mockMvc.perform(post("/api/v1/order/checkout")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_number").value("ORD-1"));
    }

    @Test
    void checkout_guest_returnsOkWithAccessToken() throws Exception {
        Order order = baseOrder();
        order.setGuest(true);
        order.setAccessToken("guest-token");
        String anonId = UUID.randomUUID().toString();
        when(identityResolver.resolve(any(), any())).thenReturn(OrderIdentity.ofGuest(anonId));
        when(checkoutService.checkout(any(OrderIdentity.class), any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/order/checkout")
                        .header("X-Anon-Id", anonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_number").value("ORD-1"))
                .andExpect(jsonPath("$.access_token").value("guest-token"));
    }
}
