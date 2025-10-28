//package com.shopfast.inventoryservice.util;
//
//import com.github.javafaker.Faker;
//import com.shopfast.inventoryservice.client.ProductClient;
//import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
//import com.shopfast.inventoryservice.model.InventoryItem;
//import com.shopfast.inventoryservice.repository.InventoryRepository;
//import com.shopfast.inventoryservice.service.InventoryService;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.UUID;
//
//@Slf4j
//@Component
//public class InventoryDataSeeder {
//
//    private static final int PRODUCT_COUNT = 1000;
//
//    private final InventoryRepository inventoryRepository;
//
//    private final ProductClient productClient;
//
//    @Autowired
//    private InventoryService inventoryService;
//
//    public InventoryDataSeeder(InventoryRepository inventoryRepository, ProductClient productClient) {
//        this.inventoryRepository = inventoryRepository;
//        this.productClient = productClient;
//    }
//
//    @Value("${app.seed-inventory:false}")
//    private boolean seedInventory; // toggle via application.yml
//
//    @PostConstruct
//    public void seed() {
//        if (!seedInventory) {
//            System.out.println("🟢 Inventory seeding disabled (set app.seed-products=true to enable)");
//            return;
//        }
//
////        if (inventoryRepository.count() > 0) {
////            System.out.println("🟢 Inventory already exist, skipping seeding.");
////            return;
//            inventoryRepository.deleteAll();
////        }
//
//        log.info("🚀 Generating " + PRODUCT_COUNT + " dummy inventory...");
//
//        List<InventoryItem> inventoryItems = new ArrayList<>();
//        List<String> productList = productClient.fetchAllProducts();
//        productList.stream().forEach(id -> {
//            log.info("Creating inventory for product Id : {}", id);
//            InventoryItem inventoryItem = InventoryItem.builder()
//                    .id(UUID.randomUUID())
//                    .productId(UUID.fromString(id))
//                    .availableQuantity(0)
//                    .reservedQuantity(0)
//                    .soldQuantity(0)
//                    .build();
//            inventoryItems.add(inventoryItem);
//        });
//
//        inventoryRepository.saveAll(inventoryItems);
//
//        inventoryItems.stream().forEach(inventoryItem -> {
//            inventoryService.adjustQuantity(inventoryItem.getProductId(), new AdjustQuantityDto(new Random().nextInt(100)));
//
//        });
//
//
//
//        System.out.println("✅ Seeded " + PRODUCT_COUNT + " inventory successfully!");
//    }
//
//}
