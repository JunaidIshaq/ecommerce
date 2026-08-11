package com.shopfast.notificationservice.sendor;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.notificationservice.model.NotificationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationSender emailNotificationSender;

    @Test
    void supportsOnlyEmailChannel() {
        NotificationEntity email = NotificationEntity.builder().channel(NotificationChannel.EMAIL).build();
        NotificationEntity sms = NotificationEntity.builder().channel(NotificationChannel.SMS).build();

        assertThat(emailNotificationSender.supports(email)).isTrue();
        assertThat(emailNotificationSender.supports(sms)).isFalse();
    }

    @Test
    void sendDelegatesToMailSender() {
        NotificationEntity entity = NotificationEntity.builder()
                .recipient("user@example.com")
                .subject("Order")
                .content("Body")
                .build();

        emailNotificationSender.send(entity);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
