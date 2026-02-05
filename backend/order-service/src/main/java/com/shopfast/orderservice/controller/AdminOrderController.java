package com.shopfast.orderservice.controller;

import com.shopfast.orderservice.dto.AdminOrderStatusDto;
import com.shopfast.orderservice.dto.PagedResponse;
import com.shopfast.orderservice.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Orders", description = "Admin Order APIs")
@RestController
@RequestMapping("/api/v1/order/internal/admin")
public class AdminOrderController {

    private final OrderRepository orderRepository;

    public AdminOrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Operation(summary = "Get order status by ID for admin")
    @GetMapping("/orders/{id}/status")
    public ResponseEntity<PagedResponse<AdminOrderStatusDto>> getOrderStatus(
            @PathVariable("id") String id,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(name = "status", required = false) String status) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminOrderStatusDto> orderPage = orderRepository.findByUserId(id, pageable)
                .map(AdminOrderStatusDto::from);

        PagedResponse<AdminOrderStatusDto> response = new PagedResponse<>(
                orderPage.getContent(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );

        return ResponseEntity.ok(response);
    }
}
