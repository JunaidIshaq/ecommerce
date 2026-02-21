package com.shopfast.adminservice.service;

import com.shopfast.adminservice.client.UserAdminClient;
import com.shopfast.adminservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserAdminClient userClient;
    private final AuditService auditService;


    public void blockUser(Long id, String adminEmail) {
        userClient.blockUser(id);
        auditService.log(adminEmail, "BLOCK_USER", "USER", id);
    }
}
