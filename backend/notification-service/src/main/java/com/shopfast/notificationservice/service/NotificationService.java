package com.shopfast.notificationservice.service;

import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.dto.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationResponseDto createAndSend(CreateNotificationRequestDto createNotificationRequestDto);

    Page<NotificationResponseDto> getUserNotifications(UUID userId, Pageable pageable);

    NotificationResponseDto markAsRead(UUID notificationId);

    void processPendingNotifications();

}
