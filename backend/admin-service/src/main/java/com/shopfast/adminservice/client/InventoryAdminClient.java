package com.shopfast.adminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url= "${inventory.service.url}", path = "/internal/admin/inventory")
public interface InventoryAdminClient {

    @PutMapping("/{productId}/stock")
    void updateStock(@PathVariable Long productId,
                     @RequestParam int quantity);

}

