package com.shopfast.orderservice.controller;

import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders", description = "Order APIs")
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Place an order")
    @PostMapping
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody OrderRequestDto dto) {
        // prefer userId from JWT
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getName();
        dto.setUserId(userId);
        Order saved = orderService.placeOrder(dto);
        return ResponseEntity.ok(saved);
    }


    @Operation(summary = "List orders for current user")
    @GetMapping
    public ResponseEntity<List<Order>> myOrders() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getOrdersForUser(userId));
    }


    @Operation(summary = "Get order by id")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        return orderService.getOrderById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Cancel order")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable UUID id) {
        Order canceled = orderService.cancelOrder(id);
        return ResponseEntity.ok(canceled);
    }

    @Operation(summary = "Confirm order")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Order> confirm(@PathVariable UUID id) {
        Order canceled = orderService.confirmOrder(id);
        return ResponseEntity.ok(canceled);
    }

}
