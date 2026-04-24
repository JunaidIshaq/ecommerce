package com.shopfast.orderservice.controller;

import com.shopfast.common.dto.AdminOrderDto;
import com.shopfast.common.dto.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Orders", description = "Admin Order APIs")
@RestController
@RequestMapping("/api/v1/order")
public class AdminOrderController {

    private final OrderRepository orderRepository;

    public AdminOrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Operation(summary = "Get order status by ID for admin")
    @GetMapping("/internal/admin/orders/pageNumber/{pageNumber}/pageSize/{pageSize}")
    public ResponseEntity<PagedResponse<AdminOrderDto>> getOrderStatus(
            @RequestHeader("userId") String userId,
            @PathVariable(name = "pageNumber", required = false) Integer pageNumber,
            @PathVariable(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "status", required = false) String status) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminOrderDto> orderPage = orderRepository.findAll(pageable)
                .map(order -> AdminOrderDto.builder()
                        .id(order.getId())
                        .userId(order.getUserId())
                        .orderNumber(order.getOrderNumber())
                        .subTotal(order.getSubTotal().toString())
                        .discount(order.getDiscount().toString())
                        .totalAmount(order.getTotalAmount().toString())
                        .status(order.getStatus().name())
                        .createdAt(order.getCreatedAt())
                        .updatedAt(order.getUpdatedAt())
                        .build());

        PagedResponse<AdminOrderDto> response = new PagedResponse<>(
                orderPage.getContent(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );

        return ResponseEntity.ok(response);
    }
}
