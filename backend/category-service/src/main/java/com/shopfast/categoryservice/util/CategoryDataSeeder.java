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
@RequiredArgsConstructor
public class CategoryDataSeeder {

    private final CategoryRepository categoryRepository;

    private final ElasticCategorySearchService elasticService;

    @Value("${app.seed-categories:false}")
    private boolean seedCategories; // toggle via application.yml

    private static final int CATEGORY_COUNT = 6;

    @PostConstruct
    public void seed() {
        if (!seedCategories) {
            System.out.println("🟢 Category seeding disabled (set app.seed-categories=true to enable)");
            return;
        }
        categoryRepository.deleteAll();
        if (categoryRepository.count() > 0) {
            System.out.println("🟢 Category already exist, skipping seeding.");
            return;
        }

        // Predefined category list with fixed UUIDs
        List<Category> predefined = List.of(
                new Category("e2273029-2a14-4d01-a410-0fd5dc9d10b2", "Electronics", "Devices, gadgets and accessories", null, null, null, null),
                new Category("a41f304e-79da-4b41-bc3e-5613d9cbf8a7", "Fashion", "Clothing, shoes and accessories", null, null, null, null),
                new Category("d91a31ab-4c5f-4fa5-8107-2e33a6c5a1b4", "Home Appliances", "Appliances for daily use", null, null, null, null),
                new Category("bcd98f12-8e64-495e-91a1-901acb2f64cd", "Books", "All kinds of books and educational materials", null, null, null, null),
                new Category("c4a03f2a-f98f-43d2-b941-62b86a12a6ef", "Health & Beauty", "Health care and beauty products", null, null, null, null),
                new Category("ec8d39ba-761a-4f07-9b8c-2b60684dcb01", "Sports", "Sports gear and accessories", null, null, null, null)
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
