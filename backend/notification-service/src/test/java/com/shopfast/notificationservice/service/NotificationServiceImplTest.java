package com.shopfast.notificationservice.service;

import com.shopfast.common.enums.NotificationChannel;
import com.shopfast.common.enums.NotificationStatus;
import com.shopfast.notificationservice.dto.CreateNotificationRequestDto;
import com.shopfast.notificationservice.dto.NotificationResponseDto;
import com.shopfast.notificationservice.model.NotificationEntity;
import com.shopfast.notificationservice.repository.NotificationRepository;
import com.shopfast.notificationservice.sendor.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender emailSender;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, List.of(emailSender));
    }

    private CreateNotificationRequestDto request() {
        return new CreateNotificationRequestDto(
                UUID.randomUUID(),
                "a@b.com",
                "Hi",
                "Hello",
                com.shopfast.common.enums.NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL);
    }

    @Test
    void createAndSendPersistsAndMarksSentOnSuccess() {
        NotificationEntity saved = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(saved);
        when(emailSender.supports(any())).thenReturn(true);

        NotificationResponseDto response = notificationService.createAndSend(request());

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
        try { verify(emailSender).send(any()); } catch (Exception ignored) {}
    }

    @Test
    void createAndSendMarksFailedWhenSenderThrows() {
        NotificationEntity saved = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(saved);
        when(emailSender.supports(any())).thenReturn(true);
        try { org.mockito.Mockito.doThrow(new RuntimeException("smtp down")).when(emailSender).send(any()); } catch (Exception ignored) {}

        NotificationResponseDto response = notificationService.createAndSend(request());

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("smtp down");
    }

    @Test
    void createAndSendThrowsWhenNoSenderSupportsChannel() {
        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenReturn(NotificationEntity.builder().id(UUID.randomUUID()).build());
        when(emailSender.supports(any())).thenReturn(false);

        assertThatThrownBy(() -> notificationService.createAndSend(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No sender found");
    }

    @Test
    void markAsReadSetsReadStatus() {
        UUID id = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(id).status(NotificationStatus.PENDING).build();
        when(notificationRepository.findById(id)).thenReturn(Optional.of(entity));

        notificationService.markAsRead(id);

        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    void markAsReadThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processPendingNotificationsSendsEachPendingEmail() {
        NotificationEntity n1 = NotificationEntity.builder().id(UUID.randomUUID()).status(NotificationStatus.PENDING).channel(NotificationChannel.EMAIL).build();
        when(notificationRepository.findByStatusAndChannel(NotificationStatus.PENDING, NotificationChannel.EMAIL))
                .thenReturn(List.of(n1));
        when(emailSender.supports(any())).thenReturn(true);

        notificationService.processPendingNotifications();

        try { verify(emailSender).send(n1); } catch (Exception ignored) {}
    }
}
