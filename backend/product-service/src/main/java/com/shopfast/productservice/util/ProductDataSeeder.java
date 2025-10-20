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
            p.setCategory(faker.commerce().department());
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
}
