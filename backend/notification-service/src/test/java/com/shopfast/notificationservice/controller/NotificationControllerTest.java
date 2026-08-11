package com.shopfast.notificationservice.controller;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.common.enums.NotificationStatus;
import com.shopfast.common.enums.NotificationType;
import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.dto.NotificationResponseDto;
import com.shopfast.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.notificationservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.notificationservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private NotificationService notificationService;

    @Test
    void createAndSend_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateNotificationRequestDto request = new CreateNotificationRequestDto(
                userId, "user@example.com", "Subject", "Content",
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL);

        NotificationResponseDto response = NotificationResponseDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipient("user@example.com")
                .status(NotificationStatus.SENT)
                .build();
        when(notificationService.createAndSend(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/notification")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipient").value("user@example.com"));
    }

    @Test
    void getUserNotifications_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResponseDto response = NotificationResponseDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notification/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void markAsRead_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationResponseDto response = NotificationResponseDto.builder()
                .id(id)
                .readAt(java.time.Instant.now())
                .build();
        when(notificationService.markAsRead(id)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notification/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
