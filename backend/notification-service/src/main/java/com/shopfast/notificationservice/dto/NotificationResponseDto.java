package com.shopfast.notificationservice.dto;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.enums.NotificationStatus;
import com.shopfast.notificationservice.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponseDto {

    private UUID id;

    private UUID userId;

    private String recipient;

    private String subject;

    private String content;

    private NotificationType type;

    private NotificationChannel channel;

    private NotificationStatus status;

    private Instant createdAt;

    private Instant sentAt;

    private Instant readAt;

    private String errorMessage;

}
