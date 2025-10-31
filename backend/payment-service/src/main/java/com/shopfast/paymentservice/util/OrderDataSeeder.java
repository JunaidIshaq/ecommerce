//package com.shopfast.paymentservice.util;
//
//import com.shopfast.paymentservice.client.ProductClient;
//import com.shopfast.paymentservice.dto.PaymentRequestDto;
//import com.shopfast.paymentservice.model.OrderItem;
//import com.shopfast.paymentservice.service.PaymentService;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.UUID;
//
//@Slf4j
//@Component
//public class OrderDataSeeder {
//
//    private final OrderRepository orderRepository;
//
//    private final PaymentService paymentService;
//
//    private final ProductClient productClient;
//
//    public OrderDataSeeder(OrderRepository orderRepository, PaymentService paymentService, ProductClient productClient) {
//        this.orderRepository = orderRepository;
//        this.paymentService = paymentService;
//        this.productClient = productClient;
//    }
//
//    @Value("${app.seed-order:false}")
//    private boolean seedOrder; // toggle via application.yml
//
//    private static final int ORDER_COUNT = 10;
//
//    @PostConstruct
//    public void seed() throws IOException {
//        if (!seedOrder) {
//            System.out.println("🟢 Order seeding disabled (set app.seed-products=true to enable)");
//            return;
//        }
//        orderRepository.deleteAll();
//        if (orderRepository.count() > 0) {
//            System.out.println("🟢 Orders already exist, skipping seeding.");
//            return;
//        }
//
//        log.info("🚀 Generating " + ORDER_COUNT + " dummy orders...");
//
//        List<String> productIds = productClient.fetchAllProducts();
//
//        for (int i = 1; i <= ORDER_COUNT; i++) {
//            PaymentRequestDto paymentRequestDto = new PaymentRequestDto();
//            paymentRequestDto.setUserId(UUID.randomUUID().toString());
//            List<OrderItem> orderItems = new ArrayList<>();
//            for(int j = 1; j <= new Random().nextInt(1,20); j++) {
//                OrderItem  orderItem = new OrderItem();
//                orderItem.setId(UUID.randomUUID());
//                orderItem.setProductId(UUID.fromString(productIds.get(new Random().nextInt(1000))));
//                orderItem.setQuantity(new Random().nextInt(1, 10));
//                orderItem.setPrice(BigDecimal.valueOf(new Random().nextDouble(1000)));
//                orderItem.setCreatedAt(Instant.now());
//                orderItem.setUpdatedAt(Instant.now());
//                orderItem.setCreatedBy(UUID.randomUUID().toString());
//                orderItem.setUpdatedBy(UUID.randomUUID().toString());
//                orderItems.add(orderItem);
//            }
//            paymentRequestDto.setItems(orderItems.stream().map(OrderMapper::getOrderItemDto).toList());
//            paymentService.placeOrder(paymentRequestDto);
//        }
//
//        System.out.println("✅ Seeded " + ORDER_COUNT + " orders successfully!");
//    }
//
//}
