package com.shopfast.adminservice.repository;

import com.shopfast.adminservice.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditRepository extends JpaRepository<AdminAuditLog, UUID> {


}
