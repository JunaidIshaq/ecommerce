package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService service;

    @PutMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id,
                             @RequestParam String status,
                             Authentication auth) {
        service.updateStatus(id, status, auth.getName());
    }
}
