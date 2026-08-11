package com.shopfast.orderservice.controller;

import com.shopfast.common.dto.AdminOrderDto;
import com.shopfast.common.dto.GenericApiResponseDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.client.ProductClient;
import com.shopfast.orderservice.dto.AdminOrderDetailDto;
import com.shopfast.orderservice.dto.AdminOrderItemDetailDto;
import com.shopfast.orderservice.dto.ProductDetailDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.repository.OrderRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminOrderController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.orderservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderRepository orderRepository;
    @MockBean private ProductClient productClient;

    private Order sampleOrder() {
        OrderItem item = OrderItem.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        return Order.builder()
                .id(UUID.randomUUID())
                .userId("user")
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
    void getOrderStatus_returnsOk() throws Exception {
        Order order = sampleOrder();
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/order/internal/admin/orders/pageNumber/1/pageSize/10")
                        .header("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getOrderById_returnsOk() throws Exception {
        Order order = sampleOrder();
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.of(order));
        when(productClient.fetchProductsByIds(anyList())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/order/internal/admin/order/{id}", order.getId())
                        .header("userId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_number").value("ORD-1"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void getOrderById_notFound_returnsNotFound() throws Exception {
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/order/internal/admin/order/{id}", UUID.randomUUID())
                        .header("userId", "admin"))
                .andExpect(status().isNotFound());
    }
}
