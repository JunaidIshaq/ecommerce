package com.shopfast.adminservice.service;

import com.shopfast.adminservice.model.AdminAuditLog;
import com.shopfast.adminservice.repository.AdminAuditRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AdminAuditRepository repo;

    public AuditService(AdminAuditRepository repo) {
        this.repo = repo;
    }

    public void log(String adminEmail, String action, String entityType, Long entityId) {
        repo.save(AdminAuditLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build());
    }


}
