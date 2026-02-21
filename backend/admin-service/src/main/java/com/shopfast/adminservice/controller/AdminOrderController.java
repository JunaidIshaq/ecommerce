package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.OrderAdminClient;
import com.shopfast.adminservice.dto.AdminOrderDto;
import com.shopfast.adminservice.dto.PagedResponse;
import com.shopfast.adminservice.service.AdminOrderService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService service;

    private final OrderAdminClient orderAdminClient;

    @GetMapping("/orders/pageNumber/{pageNumber}/pageSize/{pageSize}")
    public Object getAllOrders( @RequestHeader("userId") @NotNull String id,
                                @PathVariable Integer pageNumber,
                                @PathVariable Integer pageSize,
                                @RequestParam(required = false) String status,
                                Authentication auth) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if(StringUtils.isEmpty(userId)) {
            throw new RuntimeException("Not an Admin User");
        }

        return orderAdminClient.getAllOrders(id, pageNumber, pageSize, status);

    }
}
