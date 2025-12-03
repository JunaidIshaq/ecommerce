//package com.shopfast.elasticservice.sendor;
//
//import com.google.firebase.messaging.FirebaseMessaging;
//import com.google.firebase.messaging.FirebaseMessagingException;
//import com.google.firebase.messaging.Message;
//import com.google.firebase.messaging.Notification;
//import com.shopfast.elasticservice.enums.NotificationChannel;
//import com.shopfast.elasticservice.document.ProductDocument;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class PushNotificationSender implements NotificationSender {
//
//    @Override
//    public boolean supports(ProductDocument productDocument) {
//        return productDocument.getChannel().equals(NotificationChannel.PUSH);
//    }
//
//    @Override
//    public void send(ProductDocument productDocument) throws FirebaseMessagingException {
//        String fcmToken = productDocument.getRecipient(); // here recipient is FCM token
//
//        Notification firebaseNotification = Notification.builder()
//                .setTitle(productDocument.getSubject() != null ? productDocument.getSubject() : "Notification")
//                .setBody(productDocument.getContent())
//                .build();
//
//        Message message = Message.builder()
//                .setToken(fcmToken)
//                .setNotification(firebaseNotification)
//                .putData("notificationId", String.valueOf(productDocument.getId()))
//                .build();
//
//        String response = FirebaseMessaging.getInstance().send(message);
//        log.info("Push notification sent, response : {}", response);
//    }
//}
