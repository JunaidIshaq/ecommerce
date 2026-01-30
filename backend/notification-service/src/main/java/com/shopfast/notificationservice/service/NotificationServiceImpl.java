package com.shopfast.notificationservice.service;

import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.dto.NotificationResponseDto;
import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.enums.NotificationStatus;
import com.shopfast.notificationservice.model.NotificationEntity;
import com.shopfast.notificationservice.repository.NotificationRepository;
import com.shopfast.notificationservice.sendor.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final List<NotificationSender> notificationSenders;

    public NotificationServiceImpl(NotificationRepository notificationRepository, List<NotificationSender> notificationSenders) {
        this.notificationRepository = notificationRepository;
        this.notificationSenders = notificationSenders;
    }

    @Override
    public NotificationResponseDto createAndSend(CreateNotificationRequestDto createNotificationRequestDto) {
        NotificationEntity notificationEntity = NotificationEntity.builder()
                .userId(createNotificationRequestDto.getUserId())
                .recipient(createNotificationRequestDto.getRecipient())
                .subject(createNotificationRequestDto.getSubject())
                .content(createNotificationRequestDto.getContent())
                .type(createNotificationRequestDto.getType())
                .channel(createNotificationRequestDto.getChannel())
                .channel(createNotificationRequestDto.getChannel())
                .status(NotificationStatus.PENDING)
                .build();

        notificationEntity = notificationRepository.save(notificationEntity);

        // Try to send immediately (synchronous)
        sendNotification(notificationEntity);
        return toResponse(notificationEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    public NotificationResponseDto markAsRead(UUID notificationId) {
        NotificationEntity notificationEntity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("NotificationEntity not found with id " + notificationId));

        notificationEntity.setStatus(NotificationStatus.READ);
        notificationEntity.setReadAt(Instant.now());

        return toResponse(notificationEntity);
    }

    @Override
    public void processPendingNotifications() {
        log.info("Processing pending notifications...");
        List<NotificationEntity> pending = notificationRepository
                .findByStatusAndChannel(NotificationStatus.PENDING, NotificationChannel.EMAIL); // you can expand

        for (NotificationEntity notificationEntity : pending) {
            sendNotification(notificationEntity);
        }
    }


    private void sendNotification(NotificationEntity notificationEntity) {
        NotificationSender sender = notificationSenders.stream()
                .filter(s -> s.supports(notificationEntity))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No sender found for channel " + notificationEntity.getChannel()));

        try {
            sender.send(notificationEntity);
            notificationEntity.setStatus(NotificationStatus.SENT);
            notificationEntity.setSentAt(Instant.now());
            notificationEntity.setErrorMessage(null);
        } catch (Exception e) {
            log.error("Failed to send notificationEntity id : {}", notificationEntity.getId(), e);
            notificationEntity.setStatus(NotificationStatus.FAILED);
            notificationEntity.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(notificationEntity);
    }

    private NotificationResponseDto toResponse(NotificationEntity notificationEntity) {
        return NotificationResponseDto.builder()
                .id(notificationEntity.getId())
                .userId(notificationEntity.getUserId())
                .recipient(notificationEntity.getRecipient())
                .subject(notificationEntity.getSubject())
                .content(notificationEntity.getContent())
                .type(notificationEntity.getType())
                .channel(notificationEntity.getChannel())
                .status(notificationEntity.getStatus())
                .createdAt(notificationEntity.getCreatedAt())
                .sentAt(notificationEntity.getSentAt())
                .readAt(notificationEntity.getReadAt())
                .errorMessage(notificationEntity.getErrorMessage())
                .build();
    }

}
