package com.shopfast.notificationservice.repository;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.enums.NotificationStatus;
import com.shopfast.notificationservice.model.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<NotificationEntity> findByStatusAndChannel(NotificationStatus status, NotificationChannel channel);

}
