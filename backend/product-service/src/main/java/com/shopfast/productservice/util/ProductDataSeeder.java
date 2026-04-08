package com.shopfast.productservice.util;

import com.github.javafaker.Faker;
import com.shopfast.common.dto.CategoryDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.service.ProductService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class ProductDataSeeder {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CategoryClient categoryClient;

    @Value("${app.seed-products:false}")
    private boolean seedProducts;

    private static final int PRODUCT_COUNT = 1000;

    // Elasticsearch disabled - simplified constructor
    public ProductDataSeeder(ProductRepository productRepository,
                             ProductService productService,
                             CategoryClient categoryClient) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.categoryClient = categoryClient;
    }

    @PostConstruct
    public void seed() {
        if (!seedProducts) {
            log.info("🟢 Product seeding disabled (set app.seed-products=true to enable)");
            return;
        }

        // Elasticsearch disabled - only check database
        try {
            long dbCount = productRepository.count();
            if (dbCount > 0) {
                log.info("🟢 Products already exist, skipping seeding.");
                return;
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not count existing products, proceeding with seeding", e);
        }

        try {
            productRepository.deleteAll();

            log.info("🚀 Generating {} dummy products...", PRODUCT_COUNT);

            Faker faker = new Faker();
            Random random = new Random();

            List<String> categoryIds = getSafeCategoryList();

            for (int i = 1; i <= PRODUCT_COUNT; i++) {
                Product p = new Product();
                p.setName(faker.commerce().productName());
                p.setSlug(faker.commerce().productName().toLowerCase().replace(" ", "-"));
                p.setDescription(faker.lorem().sentence(100));
                p.setCategoryId(categoryIds.get(random.nextInt(categoryIds.size())));
                p.setPrice(new BigDecimal(faker.commerce().price(10.0, 999.99)));
                p.setStock(random.nextInt(1000));
                p.setRating(random.nextDouble() * 10);
                p.setImages(List.of("https://picsum.photos/seed/" + i + "/600/400"));
                productService.createProduct(ProductMapper.getProductDto(p));
            }

            log.info("✅ Seeded {} products successfully!", PRODUCT_COUNT);
        } catch (Exception e) {
            log.error("❌ Exception occurred when seeding products", e);
        }
    }

    /**
     * Try to fetch categories safely — return fallback IDs if category-service is down.
     */
    private List<String> getSafeCategoryList() {
        try {
            PagedResponse<CategoryDto> pagedResponse = categoryClient.getAllCategories();
            List<CategoryDto> items = pagedResponse.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("⚠️ No categories found, using fallback dummy category IDs.");
                return List.of(UUID.randomUUID().toString());
            }
            return items.stream()
                    .map(CategoryDto::getId)
                    .toList();
        } catch (Exception e) {
            log.error("⚠️ Failed to fetch categories, using fallback dummy ID.", e);
            return List.of(UUID.randomUUID().toString());
        }
    }
}
