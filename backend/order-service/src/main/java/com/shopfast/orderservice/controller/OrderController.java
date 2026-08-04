package com.shopfast.orderservice.controller;

import com.shopfast.common.dto.GenericApiResponseDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.client.CartClient;
import com.shopfast.orderservice.client.ProductClient;
import com.shopfast.orderservice.dto.CheckoutRequestDto;
import com.shopfast.orderservice.dto.OrderItemDto;
import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.dto.ProductDetailDto;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders", description = "Order APIs")
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    private final OrderRepository orderRepository;

    private final CartClient cartClient;

    private final ProductClient productClient;

    public OrderController(OrderService orderService, OrderRepository orderRepository, CartClient cartClient, ProductClient productClient) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.productClient = productClient;
    }

    // For Manual Orders Only
    @Operation(summary = "Place an order")
    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto dto) {
        // prefer userId from JWT
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getName();
        dto.setUserId(userId);
        Order saved = orderService.placeOrder(dto);
        return ResponseEntity.ok(OrderResponseDto.from(saved));
    }


    @Operation(summary = "List orders for current user")
    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponseDto>> myOrders(
            @RequestHeader("userId") String userId,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
//        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber - 1);
        Page<Order> orderPage = orderService.getOrdersForUser(userId, pageable);
        List<OrderResponseDto> items = orderPage.getContent().stream()
                .map(order -> enrichOrderResponse(OrderResponseDto.from(order), order))
                .toList();
        PagedResponse<OrderResponseDto> response = new PagedResponse<>();
        response.setItems(items);
        response.setTotalItems(orderPage.getTotalElements());
        response.setTotalPages(orderPage.getTotalPages());
        response.setPage(orderPage.getNumber());
        response.setSize(orderPage.getSize());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order by id")
    @GetMapping("/{id}")
    public ResponseEntity<GenericApiResponseDto<OrderResponseDto>> getOrder(@RequestHeader("userId") String userId, @PathVariable("id") UUID id) {
        return orderService.getOrderById(id)
                .map(order -> enrichOrderResponse(OrderResponseDto.from(order), order))
                .map(orderResponse -> ResponseEntity.ok(GenericApiResponseDto.success(orderResponse, "Order fetched successfully")))
                .orElse(ResponseEntity.ok(GenericApiResponseDto.error("Order not found", 404)));
    }

    private OrderResponseDto enrichOrderResponse(OrderResponseDto response, Order order) {
        if (order.getItems() != null) {
            // One batch call instead of one HTTP round trip per line item.
            java.util.Map<java.util.UUID, ProductDetailDto> products = productClient.fetchProductsByIds(
                    order.getItems().stream().map(i -> i.getProductId()).toList());

            response.setItems(order.getItems().stream()
                    .map(item -> {
                        ProductDetailDto product = products.get(item.getProductId());
                        return OrderItemDto.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .productName(product != null ? product.getName() : null)
                                .productSlug(product != null ? product.getSlug() : null)
                                .productDescription(product != null ? product.getDescription() : null)
                                .imageUrl(product != null ? product.getImageUrl() : null)
                                .images(product != null ? product.getImages() : null)
                                .build();
                    })
                    .toList());
        }
        return response;
    }

    @Operation(summary = "Cancel order")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancel(@PathVariable("id") UUID id) {
        Order canceled = orderService.cancelOrder(id);
        return ResponseEntity.ok(OrderResponseDto.from(canceled));
    }

    @Operation(summary = "Confirm order")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<OrderResponseDto> confirm(@PathVariable("id") UUID id) {
        Order confirmed = orderService.confirmOrder(id);
        return ResponseEntity.ok(OrderResponseDto.from(confirmed));
    }

    @Operation(summary = "Checkout order")
    @PatchMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(@RequestHeader("user_id") String userId, @RequestBody CheckoutRequestDto dto) {
        var items = cartClient.getCartInternal(userId);
//        Order order = orderService.createFromCart(userId, items, dto.getCouponCode());
//        return ResponseEntity.ok(OrderResponseDto.from(order));
        return null;
    }



}
