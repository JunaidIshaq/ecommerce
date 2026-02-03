package com.shopfast.adminservice.client;

import com.shopfast.adminservice.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "order-service", url="${order.service.url}", path = "/internal/admin/orders")
public interface OrderAdminClient {

    @GetMapping
    List<OrderDto> getAllOrders();

    @PutMapping("/{id}/status")
    void updateOrderStatus(@PathVariable Long id,
                           @RequestParam String status);

}
