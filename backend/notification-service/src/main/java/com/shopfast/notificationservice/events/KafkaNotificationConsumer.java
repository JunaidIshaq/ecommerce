package com.shopfast.notificationservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.common.events.NotificationEvent;
import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.service.NotificationService;
import com.shopfast.notificationservice.service.NotificationTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaNotificationConsumer {
    
    private final ObjectMapper objectMapper;
    
    private final NotificationService notificationService;

    private final NotificationTemplateService notificationTemplateService;
    
    public KafkaNotificationConsumer(ObjectMapper objectMapper, NotificationService notificationService, NotificationTemplateService notificationTemplateService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @KafkaListener(topics = "notification-topic")
    public void consume(String message) {
        try {
            log.info("Received notification message: {}", message);

            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);

            // subject/content not provided, use templates
//            if(event.getSubject() == null) {
                event.setSubject(notificationTemplateService.buildSubject(event));
//            }
//            if(event.getContent() == null) {
                event.setContent(notificationTemplateService.buildContent(event));
//            }
            CreateNotificationRequestDto requestDto = new CreateNotificationRequestDto();
            requestDto.setUserId(event.getUserId());
            requestDto.setRecipient(event.getRecipient());
            requestDto.setSubject(event.getSubject());
            requestDto.setContent(event.getContent());
            requestDto.setType(event.getNotificationType());
            requestDto.setChannel(event.getNotificationChannel());

            notificationService.createAndSend(requestDto);

            log.info("Processed notification event for userId : {}, type : {}", event.getUserId(), event.getNotificationType());

        } catch (Exception e) {
            log.error("Failed to process notification event", e);
        }
    }
    
    
}
