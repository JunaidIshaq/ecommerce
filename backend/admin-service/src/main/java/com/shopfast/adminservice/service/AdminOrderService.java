package com.shopfast.adminservice.service;

import com.shopfast.adminservice.client.OrderAdminClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderAdminClient orderClient;

    private final AuditService auditService;

    public void updateStatus(Long id, String status, String adminEmail) {
        orderClient.updateOrderStatus(id, status);
        auditService.log(adminEmail, "UPDATE_ORDER_STATUS", "ORDER", id);
    }
}
