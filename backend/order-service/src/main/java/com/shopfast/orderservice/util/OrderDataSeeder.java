package com.shopfast.orderservice.util;

import com.shopfast.orderservice.client.ProductClient;
import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class OrderDataSeeder {

    private final OrderRepository orderRepository;

    private final OrderService orderService;

    private final ProductClient productClient;

    public OrderDataSeeder(OrderRepository orderRepository, OrderService orderService, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.productClient = productClient;
    }

    @Value("${app.seed-order:false}")
    private boolean seedOrder; // toggle via application.yml

    private static final int ORDER_COUNT = 100;

    @PostConstruct
    public void seed() throws IOException {
        if (!seedOrder) {
            System.out.println("🟢 Order seeding disabled (set app.seed-products=true to enable)");
            return;
        }

        if (orderRepository.count() > 0) {
            System.out.println("🟢 Orders already exist, skipping seeding.");
            return;
        }

        log.info("🚀 Generating " + ORDER_COUNT + " dummy orders...");

        List<String> productIds = productClient.fetchAllProducts();

        for (int i = 1; i <= ORDER_COUNT; i++) {
            OrderRequestDto orderRequestDto = new OrderRequestDto();
            orderRequestDto.setUserId(UUID.randomUUID().toString());
            List<OrderItem> orderItems = new ArrayList<>();
            for(int j = 1; j <= new Random().nextInt(20); j++) {
                OrderItem  orderItem = new OrderItem();
                orderItem.setId(UUID.randomUUID());
                orderItem.setProductId(UUID.fromString(productIds.get(new Random().nextInt(1000))));
                orderItem.setQuantity(new Random().nextInt(10));
                orderItem.setPrice(BigDecimal.valueOf(new Random().nextDouble(1000)));
                orderItem.setCreatedAt(Instant.now());
                orderItem.setUpdatedAt(Instant.now());
                orderItem.setCreatedBy(UUID.randomUUID().toString());
                orderItem.setUpdatedBy(UUID.randomUUID().toString());
                orderItems.add(orderItem);
            }
            orderRequestDto.setItems(orderItems.stream().map(OrderMapper::getOrderDto).toList());
            orderService.placeOrder(orderRequestDto);
        }

        System.out.println("✅ Seeded " + ORDER_COUNT + " orders successfully!");
    }

}
