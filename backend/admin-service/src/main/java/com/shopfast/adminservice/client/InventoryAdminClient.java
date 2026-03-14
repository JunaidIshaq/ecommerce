package com.shopfast.adminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url= "${inventory.service.url}", path = "/api/v1/inventory")
public interface InventoryAdminClient {

    @GetMapping("/internal/admin/inventory/pageNumber/{pageNumber}/pageSize/{pageSize}")
    Object getAllInventory(@RequestHeader("userId") String userId, @PathVariable Integer pageNumber, @PathVariable Integer pageSize);

    @PutMapping("/{id}/status")
    void updateOrderStatus(@PathVariable Long id,
                           @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
                           @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam String status);

}

