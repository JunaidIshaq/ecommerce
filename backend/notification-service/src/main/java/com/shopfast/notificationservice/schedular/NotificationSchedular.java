package com.shopfast.notificationservice.schedular;

import com.shopfast.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationSchedular {

    private final NotificationService notificationService;

    public NotificationSchedular(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Every hour (adjust as needed)
    @Scheduled(fixedRate = 3600000)
    public void processPendingNotifications() {
        log.info("Running scheduled job to process pending notifications...");
        notificationService.processPendingNotifications();
    }
}
