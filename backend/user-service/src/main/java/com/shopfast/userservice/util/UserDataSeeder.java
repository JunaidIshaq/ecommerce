package com.shopfast.userservice.util;

import com.shopfast.userservice.client.ProductClient;
import com.shopfast.userservice.repository.UserRepository;
import com.shopfast.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserDataSeeder {

    private final UserRepository userRepository;

    private final UserService userService;

    private final ProductClient productClient;

    public UserDataSeeder(UserRepository userRepository, UserService userService, ProductClient productClient) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.productClient = productClient;
    }

    @Value("${app.seed-user:false}")
    private boolean seedOrder; // toggle via application.yml

    private static final int ORDER_COUNT = 10;

//    @PostConstruct
//    public void seed() throws IOException {
//        if (!seedOrder) {
//            System.out.println("🟢 User seeding disabled (set app.seed-products=true to enable)");
//            return;
//        }
//        userRepository.deleteAll();
//        if (userRepository.count() > 0) {
//            System.out.println("🟢 Orders already exist, skipping seeding.");
//            return;
//        }
//
//        log.info("🚀 Generating " + ORDER_COUNT + " dummy orders...");
//
//        List<String> productIds = productClient.fetchAllProducts();
//
//        for (int i = 1; i <= ORDER_COUNT; i++) {
//            RegisterRequestDto registerRequestDto = new RegisterRequestDto();
//            registerRequestDto.setUserId(UUID.randomUUID().toString());
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
//            registerRequestDto.setItems(orderItems.stream().map(OrderMapper::getOrderItemDto).toList());
//            userService.placeOrder(registerRequestDto);
//        }
//
//        System.out.println("✅ Seeded " + ORDER_COUNT + " orders successfully!");
//    }

}
