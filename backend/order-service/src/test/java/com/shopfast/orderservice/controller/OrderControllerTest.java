package com.shopfast.orderservice.controller;

import com.shopfast.common.dto.GenericApiResponseDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.client.CartClient;
import com.shopfast.orderservice.client.ProductClient;
import com.shopfast.orderservice.dto.OrderItemDto;
import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.dto.ProductDetailDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.orderservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.orderservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderService orderService;
    @MockBean private OrderRepository orderRepository;
    @MockBean private CartClient cartClient;
    @MockBean private ProductClient productClient;

    @BeforeEach
    void auth() {
        var a = new UsernamePasswordAuthenticationToken("user", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(a);
    }

    private Order sampleOrder(String userId) {
        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderNumber("ORD-1")
                .status(OrderStatus.PENDING)
                .paymentStatus(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .subTotal(new BigDecimal("20.00"))
                .discount(new BigDecimal("0"))
                .totalAmount(new BigDecimal("20.00"))
                .items(List.of(item))
                .build();
    }

    @Test
    void placeOrder_returnsOk() throws Exception {
        Order saved = sampleOrder("user");
        when(orderService.placeOrder(any(OrderRequestDto.class))).thenReturn(saved);

        OrderItemDto item = OrderItemDto.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        OrderRequestDto request = new OrderRequestDto();
        request.setUserId("user");
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_number").value("ORD-1"));
    }

    @Test
    void placeOrder_invalidBody_returnsBadRequest() throws Exception {
        OrderRequestDto request = new OrderRequestDto();
        request.setUserId("user");
        request.setItems(List.of());

        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myOrders_returnsOk() throws Exception {
        Order order = sampleOrder("user");
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderService.getOrdersForUser(anyString(), any(Pageable.class))).thenReturn(page);
        when(productClient.fetchProductsByIds(anyList())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/order")
                        .header("userId", "user")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getOrder_returnsOk() throws Exception {
        Order order = sampleOrder("user");
        when(orderService.getOrderById(any(UUID.class))).thenReturn(java.util.Optional.of(order));
        when(productClient.fetchProductsByIds(anyList())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/order/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_number").value("ORD-1"));
    }

    @Test
    void getOrder_notOwned_returnsNotFoundPayload() throws Exception {
        Order order = sampleOrder("other");
        when(orderService.getOrderById(any(UUID.class))).thenReturn(java.util.Optional.of(order));
        when(productClient.fetchProductsByIds(anyList())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/order/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void cancel_returnsOk() throws Exception {
        Order order = sampleOrder("user");
        when(orderService.cancelOrder(any(UUID.class))).thenReturn(order);

        mockMvc.perform(patch("/api/v1/order/{id}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_number").value("ORD-1"));
    }

    @Test
    void confirm_returnsOk() throws Exception {
        Order order = sampleOrder("user");
        when(orderService.confirmOrder(any(UUID.class))).thenReturn(order);

        mockMvc.perform(patch("/api/v1/order/{id}/confirm", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_number").value("ORD-1"));
    }
}
