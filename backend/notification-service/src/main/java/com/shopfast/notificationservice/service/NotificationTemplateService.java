package com.shopfast.notificationservice.service;

import com.shopfast.common.events.NotificationEvent;
import com.shopfast.notificationservice.enums.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {

    public String buildSubject(NotificationEvent notificationEvent) {
        NotificationType notificationType = notificationEvent.getNotificationType();
        return switch (notificationType) {
            case ORDER_CREATED ->  "Order placed successfully";
            case ORDER_PAID ->  "Payment received";
            case ORDER_SHIPPED -> "Your order is on the way";
            case ORDER_DELIVERED -> "Order delivered";
            case ORDER_CANCELLED -> "Order cancelled";
            case PASSWORD_RESET -> "Password reset request";
            case PROMOTION -> "Special offer just for you";
            default -> notificationEvent.getSubject() != null ? notificationEvent.getSubject() : "NotificationEntity";
        };
    }


    public String buildContent(NotificationEvent notificationEvent) {
        NotificationType notificationType = notificationEvent.getNotificationType();
        String referencedId = notificationEvent.getReferenceId();

        return switch (notificationType) {
            case ORDER_CREATED -> """
                    Hi,
                       \s
                        Thank you for your order.
                        Your order #%s has been placed successfully.
                       \s
                        Regards
                        ShopFast
                   \s""".formatted(referencedId);
            case ORDER_SHIPPED -> """
                    Hi,
                        \s
                        Good news ! Your order #%s has been shipped.
                        \s
                        Regards
                        ShopFast
                   \s""".formatted(referencedId);

            case ORDER_DELIVERED -> """
                    Hi,
                        \s
                        Your order #%s has been delivered. We hope you enjoy it!
                       \s
                        Regards
                        ShopFast
                   \s""".formatted(referencedId);

            case ORDER_CANCELLED -> """
                    Hi,
                        \s
                        Your order #%s has been cancelled!
                       \s
                        Regards
                        ShopFast
                   \s""".formatted(referencedId);

            case PASSWORD_RESET -> """
                    Hi,
                       \s
                       A password reset was requested for your account.
                       If this was you, please follow the link provided in the app.
                      \s
                       Regards
                       ShopFast
                   \s""";
            default -> notificationEvent.getContent(); // if already prepared
        };
    }
}
