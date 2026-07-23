package com.shopfast.orderservice.controller;

import com.shopfast.common.dto.AdminOrderDto;
import com.shopfast.common.dto.GenericApiResponseDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.client.ProductClient;
import com.shopfast.orderservice.dto.AdminOrderDetailDto;
import com.shopfast.orderservice.dto.AdminOrderItemDetailDto;
import com.shopfast.orderservice.dto.ProductDetailDto;
import com.shopfast.orderservice.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin Orders", description = "Admin Order APIs")
@RestController
@RequestMapping("/api/v1/order")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public AdminOrderController(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
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

    @Operation(summary = "Get a single order by ID for admin with product details")
    @GetMapping("/internal/admin/order/{id}")
    public ResponseEntity<GenericApiResponseDto<AdminOrderDetailDto>> getOrderById(
            @RequestHeader("userId") String userId,
            @PathVariable("id") UUID id) {

        return orderRepository.findById(id)
                .map(order -> {
                    List<AdminOrderItemDetailDto> enrichedItems = order.getItems().stream()
                            .map(item -> {
                                ProductDetailDto product = productClient.fetchProductById(item.getProductId());
                                return AdminOrderItemDetailDto.builder()
                                        .productId(item.getProductId())
                                        .productName(product != null ? product.getName() : null)
                                        .productSlug(product != null ? product.getSlug() : null)
                                        .productDescription(product != null ? product.getDescription() : null)
                                        .quantity(item.getQuantity())
                                        .price(item.getPrice())
                                        .imageUrl(product != null && product.getImages() != null && !product.getImages().isEmpty() 
                                                ? product.getImages().get(0) 
                                                : (product != null ? product.getImageUrl() : null))
                                        .images(product != null ? product.getImages() : null)
                                        .build();
                            })
                            .toList();

                    AdminOrderDetailDto detail = AdminOrderDetailDto.builder()
                            .id(order.getId())
                            .userId(order.getUserId())
                            .orderNumber(order.getOrderNumber())
                            .subTotal(order.getSubTotal().toString())
                            .discount(order.getDiscount() != null ? order.getDiscount().toString() : null)
                            .totalAmount(order.getTotalAmount().toString())
                            .status(order.getStatus().name())
                            .items(enrichedItems)
                            .createdAt(order.getCreatedAt())
                            .updatedAt(order.getUpdatedAt())
                            .build();

                    return ResponseEntity.ok(
                            GenericApiResponseDto.success(detail, "Order fetched successfully"));
                })
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(GenericApiResponseDto.error("Order not found with id: " + id, 404)));
    }
}
