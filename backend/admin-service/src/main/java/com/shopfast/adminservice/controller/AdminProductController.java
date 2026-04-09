package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.OrderAdminClient;
import com.shopfast.adminservice.client.ProductAdminClient;
import com.shopfast.adminservice.service.AdminOrderService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminProductController {


    private final AdminOrderService service;

    private final ProductAdminClient productAdminClient;

    @GetMapping("/product/pageNumber/{pageNumber}/pageSize/{pageSize}")
    public Object getAllProducts( @RequestHeader("userId") @NotNull String id,
                                @PathVariable("pageNumber") Integer pageNumber,
                                @PathVariable("pageSize") Integer pageSize,
                                Authentication auth) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if(StringUtils.isEmpty(userId)) {
            throw new RuntimeException("Not an Admin User");
        }

        return productAdminClient.getAllProducts(id, pageNumber, pageSize);

    }
}
