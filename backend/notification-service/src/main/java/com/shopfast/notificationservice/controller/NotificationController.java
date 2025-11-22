package com.shopfast.notificationservice.controller;

import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.dto.NotificationResponseDto;
import com.shopfast.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;


    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponseDto createAndSend(@Valid @RequestBody CreateNotificationRequestDto createNotificationRequestDto) {
        return notificationService.createAndSend(createNotificationRequestDto);
    }

    @GetMapping("/user/{userId}")
    public Page<NotificationResponseDto> getUserNotifications(@PathVariable UUID userId,
                                                              @RequestParam(defaultValue = "1") Integer pageNumber,
                                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return notificationService.getUserNotifications(userId, PageRequest.of(pageNumber - 1, pageSize));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponseDto markAsRead(@PathVariable UUID userId) {
        return notificationService.markAsRead(userId);
    }
}
