package com.shopfast.paymentservice.controller;

import com.shopfast.paymentservice.config.StripeProperties;
import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.dto.PaymentResponseDto;
import com.shopfast.paymentservice.enums.PaymentMethod;
import com.shopfast.paymentservice.enums.PaymentStatus;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.paymentservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.paymentservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PaymentService paymentService;
    @MockBean private StripeProperties stripeProperties;

    @Test
    void createPayment_returnsOk() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(UUID.randomUUID());
        dto.setUserId(UUID.randomUUID());
        dto.setAmount(100.0);
        dto.setMethod(PaymentMethod.CARD);

        Payment saved = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
                .amount(100.0)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.INITIATED)
                .clientSecret("secret")
                .transactionId("txn")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequestDto.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/payment")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void createPayment_invalidBody_returnsBadRequest() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto();

        mockMvc.perform(post("/api/v1/payment")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(50.0)
                .method(PaymentMethod.STRIPE)
                .status(PaymentStatus.SUCCESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentService.getById(id)).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payment/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void webhook_invalidSignature_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payment/webhook")
                        .header("Stripe-Signature", "bad-signature")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
