package com.shopfast.categoryservice.util;

import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import com.shopfast.categoryservice.search.ElasticCategorySearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CategoryDataSeeder {

    private final CategoryRepository categoryRepository;

    private final ElasticCategorySearchService elasticService;

    public CategoryDataSeeder(CategoryRepository categoryRepository, ElasticCategorySearchService elasticService) {
        this.categoryRepository = categoryRepository;
        this.elasticService = elasticService;
    }

    @Value("${app.seed-categories:false}")
    private boolean seedCategories; // toggle via application.yml

    private static final int CATEGORY_COUNT = 6;

    @PostConstruct
    public void seed() {
        if (!seedCategories) {
            System.out.println("🟢 Category seeding disabled (set app.seed-categories=true to enable)");
            return;
        }

        if (categoryRepository.count() > 0) {
            System.out.println("🟢 Category already exist, skipping seeding.");
            return;
        }

        // Predefined category list with fixed UUIDs
        List<Category> predefined = List.of(
                new Category("e2273029-2a14-4d01-a410-0fd5dc9d10b2", "Electronics", "Devices, gadgets and accessories", null, null, null, null, null, null ),
                new Category("a41f304e-79da-4b41-bc3e-5613d9cbf8a7", "Fashion", "Clothing, shoes and accessories", null, null, null, null, null, null),
                new Category("d91a31ab-4c5f-4fa5-8107-2e33a6c5a1b4", "Home Appliances", "Appliances for daily use", null, null, null, null, null, null),
                new Category("bcd98f12-8e64-495e-91a1-901acb2f64cd", "Books", "All kinds of books and educational materials", null, null, null, null, null, null),
                new Category("c4a03f2a-f98f-43d2-b941-62b86a12a6ef", "Health & Beauty", "Health care and beauty products", null, null, null, null, null, null),
                new Category("ec8d39ba-761a-4f07-9b8c-2b60684dcb01", "Sports", "Sports gear and accessories", null, null, null, null, null, null),
                new Category("f47a3b18-9db7-4a6a-931e-5276c7f8284e", "Groceries", "Daily grocery items", null, null, null, null, null, null),
                new Category("da24b31e-2e91-4a49-b830-308a971e6a2d", "Toys", "Toys for kids and adults", null, null, null, null, null, null),
                new Category("b8c572f3-fb8e-41f9-b6cc-f5f33f7dc9ea", "Automotive", "Car accessories and parts", null, null, null, null, null, null),
                new Category("a75cda0f-0f40-4f56-bf41-4eb556c5162f", "Furniture", "Home and office furniture", null, null, null, null, null, null),
                new Category("ef3a9d3c-b32d-4bda-9e8b-3eae6b5de99f", "Jewelry", "Rings, necklaces, watches", null, null, null, null, null, null),
                new Category("bb45a29b-18d2-4cc9-89c9-10fbb2d6db34", "Pet Supplies", "Pet food and accessories", null, null, null, null, null, null),
                new Category("e14c64b5-d631-4d9a-b6f5-8f83a2d3a7f5", "Stationery", "Office and school supplies", null, null, null, null, null, null),
                new Category("db672ac1-77f5-48df-97f1-d42b6b55d0a3", "Garden", "Outdoor and gardening tools", null, null, null, null, null, null),
                new Category("cf0e5b78-92f4-4374-9953-8718f90b0194", "Music", "Instruments and audio gear", null, null, null, null, null, null),
                new Category("de12e7ac-f745-47e4-89b8-21a7dc7b16f7", "Movies & Games", "DVDs, Blu-rays, and gaming", null, null, null, null, null, null),
                new Category("d9b72324-37cd-4d3a-90a9-231a60e6fdb5", "Art & Crafts", "Craft supplies and artworks", null, null, null, null, null, null),
                new Category("a56e23b3-0adf-4933-8d54-36b1f4c7c920", "Office Equipment", "Business and office essentials", null, null, null, null, null, null),
                new Category("e5b4e4d7-bb98-421a-8ee5-dae2bfa8a9c2", "Baby Products", "Baby food, toys, accessories", null, null, null, null, null, null),
                new Category("a1e6a68c-8423-4ed8-9da0-2c8d5e25b9ef", "Travel", "Luggage and travel accessories", null, null, null, null, null, null),
                new Category("b9c32a48-51f1-4c3a-8a1a-9db63e6a57f1", "Tools & Hardware", "Industrial and home tools", null, null, null, null, null, null),
                new Category("a48f99ec-c02e-4e37-bbda-8abf79a0135f", "Cleaning Supplies", "Household cleaning essentials", null, null, null, null, null, null),
                new Category("d63b1af7-1f1f-42b3-96e7-64e4a8f3731b", "Software", "Applications and licenses", null, null, null, null, null, null),
                new Category("bcb2e19f-d4e1-4df2-8b2a-351a4d3b2f71", "Collectibles", "Rare and collectible items", null, null, null, null, null, null),
                new Category("a2f93031-3b19-4a65-9a85-c3f02f986cb8", "Outdoor", "Camping and adventure gear", null, null, null, null, null, null),
                new Category("f73c05ac-64a3-4cbf-8a7f-445a1e9f76e9", "Smart Home", "IoT and automation devices", null, null, null, null, null, null),
                new Category("c5f37d9c-6a11-4a47-93c3-13e8a09c6e3f", "Gifts", "Gift items for all occasions", null, null, null, null, null, null),
                new Category("e16e5c4d-0022-41c8-b6df-243fe57d36f2", "Footwear", "Shoes, sandals and boots", null, null, null, null, null, null),
                new Category("d67bfa44-1974-4b68-89cd-f5c58f16e11e", "Watches", "Smart and analog watches", null, null, null, null, null, null),
                new Category("fa3c2c11-c8cc-4e8f-bc61-1371af18cf9b", "Gaming", "Consoles and accessories", null, null, null, null, null, null)
        );

        List<Category> categories = new ArrayList<>();

        predefined.forEach(category -> {
            if (!categoryRepository.existsById(category.getId())) {
                categories.add(category);
                log.info("✅ Inserted category: {} ({})", category.getName(), category.getId());
            } else {
                log.info("ℹ️ Category already exists: {}", category.getName());
            }
        });
        // Saving Categories
        categoryRepository.saveAll(categories);

        // Index in Elasticsearch
        categories.forEach(category -> {
            try {
                elasticService.indexCategory(category);
            } catch (IOException e) {
                log.error("⚠️ Failed to index category: " + category.getName());
            }
        });

        log.info("✅ Seeded " + CATEGORY_COUNT + " categories successfully!");
    }
}
