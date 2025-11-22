package com.shopfast.notificationservice.sendor;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.model.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    @Override
    public boolean supports(NotificationEntity notificationEntity) {
        return notificationEntity.getChannel() == NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationEntity notificationEntity) {
        log.info("Sending email notificationEntity id : {} to : {}", notificationEntity.getId(), notificationEntity.getRecipient());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notificationEntity.getRecipient());
        message.setSubject(notificationEntity.getSubject() != null ? notificationEntity.getSubject() : "NotificationEntity");
        message.setText(notificationEntity.getContent());

        mailSender.send(message);
    }
}
