package com.shopfast.notificationservice.sendor;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.shopfast.notificationservice.model.NotificationEntity;
import jakarta.validation.constraints.NotNull;

public interface NotificationSender {

    boolean supports(@NotNull NotificationEntity notificationEntity);

    void send(@NotNull NotificationEntity notificationEntity) throws FirebaseMessagingException;

}
