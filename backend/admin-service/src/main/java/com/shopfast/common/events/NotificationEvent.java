package com.shopfast.common.events;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.common.enums.NotificationType;
import lombok.Data;

@Data
public class NotificationEvent {

    private String userId;

    private String recipient;

    private String subject;

    private String content;

    private NotificationType notificationType;

    private NotificationChannel notificationChannel;

    private String eventSource; // e.g. "order-service"

    private String referenceId; // orderId, userId, etc.

}
