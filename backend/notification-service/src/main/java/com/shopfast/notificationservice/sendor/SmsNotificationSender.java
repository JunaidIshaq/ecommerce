package com.shopfast.notificationservice.sendor;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.model.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationSender implements NotificationSender {

    @Override
    public boolean supports(NotificationEntity notificationEntity) {
        return notificationEntity.getChannel() == NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationEntity notificationEntity) {
        // TODO integrate with SMS provider
        log.info("Simulating SMS send to {} with content : {}", notificationEntity.getRecipient(), notificationEntity.getContent());
    }
}
