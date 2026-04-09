package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.InventoryAdminClient;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryAdminClient inventoryAdminClient;

    @GetMapping("/inventory/pageNumber/{pageNumber}/pageSize/{pageSize}")
    public Object getAllInventory(@RequestHeader("userId") @NotNull String id,
                                   @PathVariable("pageNumber") Integer pageNumber,
                                   @PathVariable("pageSize") Integer pageSize,
                                   Authentication auth) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (StringUtils.isEmpty(userId)) {
            throw new RuntimeException("Not an Admin User");
        }

        return inventoryAdminClient.getAllInventory(id, pageNumber, pageSize);
    }

}
