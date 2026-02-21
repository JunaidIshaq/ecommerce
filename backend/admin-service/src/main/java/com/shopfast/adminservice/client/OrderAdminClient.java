package com.shopfast.adminservice.client;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url="${order.service.url}", path = "/api/v1/order")
public interface OrderAdminClient {

    @GetMapping("/internal/admin/orders/pageNumber/{pageNumber}/pageSize/{pageSize}")
    Object getAllOrders(@RequestHeader("userId") String id, @PathVariable Integer pageNumber, @PathVariable Integer pageSize, @RequestParam String role);

    @PutMapping("/{id}/status")
    void updateOrderStatus(@PathVariable Long id,
                           @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
                           @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam String status);

}
