package com.shopfast.notificationservice.sendor;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.model.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PushNotificationSender implements NotificationSender {
    
    @Override
    public boolean supports(NotificationEntity notificationEntity) {
        return notificationEntity.getChannel().equals(NotificationChannel.PUSH);
    }

    @Override
    public void send(NotificationEntity notificationEntity) throws FirebaseMessagingException {
        String fcmToken = notificationEntity.getRecipient(); // here recipient is FCM token
        
        Notification firebaseNotification = Notification.builder()
                .setTitle(notificationEntity.getSubject() != null ? notificationEntity.getSubject() : "Notification")
                .setBody(notificationEntity.getContent())
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(firebaseNotification)
                .putData("notificationId", String.valueOf(notificationEntity.getId()))
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        log.info("Push notification sent, response : {}", response);
    }
}
