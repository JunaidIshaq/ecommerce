package com.shopfast.common.events;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.enums.NotificationType;
import lombok.Data;

import java.util.UUID;

@Data
public class NotificationEvent {

    private UUID userId;

    private String recipient;

    private String subject;

    private String content;

    private NotificationType notificationType;

    private NotificationChannel notificationChannel;

    private String eventSource; // e.g. "order-service"

    private String referenceId; // orderId, userId, etc.

}
