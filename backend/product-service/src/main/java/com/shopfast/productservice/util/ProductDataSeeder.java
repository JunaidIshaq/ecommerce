package com.shopfast.productservice.util;

import com.github.javafaker.Faker;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.search.ElasticProductSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataSeeder {

    private final ProductRepository productRepository;

    private final ElasticProductSearchService elasticService;

    @Value("${app.seed-products:false}")
    private boolean seedProducts; // toggle via application.yml

    private static final int PRODUCT_COUNT = 1000;

    @PostConstruct
    public void seed() {
        if (!seedProducts) {
            System.out.println("🟢 Product seeding disabled (set app.seed-products=true to enable)");
            return;
        }

        if (productRepository.count() > 0) {
            System.out.println("🟢 Products already exist, skipping seeding.");
            return;
        }

        log.info("🚀 Generating " + PRODUCT_COUNT + " dummy products...");
        Faker faker = new Faker();
        Random random = new Random();

        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            Product p = new Product();
            p.setName(faker.commerce().productName());
            p.setDescription(faker.lorem().sentence(100));
            p.setCategoryId(getCategoriesList(random.nextInt(30)));
            p.setPrice(new BigDecimal(faker.commerce().price(10.0, 999.99)));
            p.setStock(random.nextInt(1000));
            p.setImages(List.of("https://picsum.photos/seed/" + i + "/600/400"));
            products.add(p);
        }

        productRepository.saveAll(products);

        // Index in Elasticsearch
        products.forEach(product -> {
            try {
                elasticService.indexProduct(product);
            } catch (IOException e) {
                System.err.println("⚠️ Failed to index product: " + product.getName());
            }
        });

        System.out.println("✅ Seeded " + PRODUCT_COUNT + " products successfully!");
    }

    public String getCategoriesList(int i) {
        List<String> predefined = List.of(
                "e2273029-2a14-4d01-a410-0fd5dc9d10b2",
                "a41f304e-79da-4b41-bc3e-5613d9cbf8a7",
                "d91a31ab-4c5f-4fa5-8107-2e33a6c5a1b4",
                "bcd98f12-8e64-495e-91a1-901acb2f64cd",
                "c4a03f2a-f98f-43d2-b941-62b86a12a6ef",
                "ec8d39ba-761a-4f07-9b8c-2b60684dcb01",
                "f47a3b18-9db7-4a6a-931e-5276c7f8284e",
                "da24b31e-2e91-4a49-b830-308a971e6a2d",
                "b8c572f3-fb8e-41f9-b6cc-f5f33f7dc9ea",
                "a75cda0f-0f40-4f56-bf41-4eb556c5162f",
                "ef3a9d3c-b32d-4bda-9e8b-3eae6b5de99f",
                "bb45a29b-18d2-4cc9-89c9-10fbb2d6db34",
                "e14c64b5-d631-4d9a-b6f5-8f83a2d3a7f5",
                "db672ac1-77f5-48df-97f1-d42b6b55d0a3",
                "cf0e5b78-92f4-4374-9953-8718f90b0194",
                "de12e7ac-f745-47e4-89b8-21a7dc7b16f7",
                "d9b72324-37cd-4d3a-90a9-231a60e6fdb5",
                "a56e23b3-0adf-4933-8d54-36b1f4c7c920",
                "e5b4e4d7-bb98-421a-8ee5-dae2bfa8a9c2",
                "a1e6a68c-8423-4ed8-9da0-2c8d5e25b9ef",
                "b9c32a48-51f1-4c3a-8a1a-9db63e6a57f1",
                "a48f99ec-c02e-4e37-bbda-8abf79a0135f",
                "d63b1af7-1f1f-42b3-96e7-64e4a8f3731b",
                "bcb2e19f-d4e1-4df2-8b2a-351a4d3b2f71",
                "a2f93031-3b19-4a65-9a85-c3f02f986cb8",
                "f73c05ac-64a3-4cbf-8a7f-445a1e9f76e9",
                "c5f37d9c-6a11-4a47-93c3-13e8a09c6e3f",
                "e16e5c4d-0022-41c8-b6df-243fe57d36f2",
                "d67bfa44-1974-4b68-89cd-f5c58f16e11e",
                "fa3c2c11-c8cc-4e8f-bc61-1371af18cf9b",
                "fa3c2c11-c8cc-4e8f-bc61-1371af18cf9c"
        );
      return predefined.get(i);
    }

}
