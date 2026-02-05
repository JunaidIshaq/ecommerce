package com.shopfast.adminservice.client;

import com.shopfast.adminservice.dto.AdminOrderDto;
import com.shopfast.adminservice.dto.OrderDto;
import com.shopfast.adminservice.dto.PagedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "order-service", url="${order.service.url}", path = "/api/v1/order")
public interface OrderAdminClient {

    @GetMapping("/internal/admin/orders/{id}")
    Object getAllOrders(@PathVariable String id, @RequestParam Integer pageNumber, @RequestParam Integer pageSize, @RequestParam String status);

    @PutMapping("/{id}/status")
    void updateOrderStatus(@PathVariable Long id,
                           @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
                           @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam String status);

}
