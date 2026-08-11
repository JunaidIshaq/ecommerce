package com.shopfast.notificationservice.service;

import com.shopfast.common.enums.NotificationType;
import com.shopfast.common.events.NotificationEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationTemplateService} which builds subject/content
 * for notification events (including review/order lifecycle notifications).
 */
class NotificationTemplateServiceTest {

    private final NotificationTemplateService service = new NotificationTemplateService();

    private NotificationEvent event(NotificationType type, String referenceId) {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationType(type);
        event.setReferenceId(referenceId);
        return event;
    }

    @Test
    void buildSubjectReturnsOrderCreatedLabel() {
        assertThat(service.buildSubject(event(NotificationType.ORDER_CREATED, "ORD-1")))
                .isEqualTo("Order placed successfully");
    }

    @Test
    void buildSubjectReturnsPasswordResetLabel() {
        assertThat(service.buildSubject(event(NotificationType.PASSWORD_RESET, null)))
                .isEqualTo("Password reset request");
    }

    @Test
    void buildSubjectFallsBackToProvidedSubject() {
        NotificationEvent event = event(NotificationType.GENERIC, null);
        event.setSubject("Custom subject");
        assertThat(service.buildSubject(event)).isEqualTo("Custom subject");
    }

    @Test
    void buildContentIncludesReferenceIdForOrderCreated() {
        String content = service.buildContent(event(NotificationType.ORDER_CREATED, "ORD-42"));
        assertThat(content).contains("ORD-42");
    }

    @Test
    void buildContentIncludesReferenceIdForOrderShipped() {
        String content = service.buildContent(event(NotificationType.ORDER_SHIPPED, "ORD-7"));
        assertThat(content).contains("ORD-7");
    }

    @Test
    void buildContentReturnsProvidedContentWhenNoTemplate() {
        NotificationEvent event = event(NotificationType.GENERIC, null);
        event.setContent("Already prepared body");
        assertThat(service.buildContent(event)).isEqualTo("Already prepared body");
    }
}
