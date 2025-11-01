package com.shopfast.productservice.util;

import com.github.javafaker.Faker;
import com.shopfast.common.dto.CategoryDto;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.productservice.config.ElasticIndexConfig;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.search.ElasticProductSearchService;
import com.shopfast.productservice.service.ProductService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductDataSeeder {

    private final ProductRepository productRepository;

    private final ElasticProductSearchService elasticService;

    private final ElasticIndexConfig elasticIndexConfig;

    private final ProductService productService;
    
    private final CategoryClient categoryClient;

    public ProductDataSeeder(ElasticIndexConfig elasticIndexConfig, ElasticProductSearchService elasticService, ProductRepository productRepository, ProductService productService, CategoryClient categoryClient) {
        this.elasticIndexConfig = elasticIndexConfig;
        this.elasticService = elasticService;
        this.productRepository = productRepository;
        this.productService = productService;
        this.categoryClient = categoryClient;
    }

    @Value("${app.seed-products:false}")
    private boolean seedProducts; // toggle via application.yml

    private static final int PRODUCT_COUNT = 1000;

    @PostConstruct
    public void seed() throws IOException {
        if (!seedProducts) {
            System.out.println("🟢 Product seeding disabled (set app.seed-products=true to enable)");
            return;
        }

        if (productRepository.count() > 0 && elasticService.count() > 0) {
            System.out.println("🟢 Products already exist, skipping seeding.");
            return;
        }else{
            productRepository.deleteAll();
            elasticService.deleteAllProducts();
        }

        log.info("🚀 Generating " + PRODUCT_COUNT + " dummy products...");
        Faker faker = new Faker();
        Random random = new Random();

        elasticIndexConfig.resetProductIndex();
        elasticIndexConfig.createProductIndexIfNotExists();

        List<String> categoryIds = getCategoriesList();

        for (int i = 1; i <= PRODUCT_COUNT; i++) {
            Product p = new Product();
            p.setName(faker.commerce().productName());
            p.setSlug(faker.commerce().productName());
            p.setDescription(faker.lorem().sentence(100));
            p.setCategoryId(categoryIds.get(new Random().nextInt(categoryIds.size())));
            p.setPrice(new BigDecimal(faker.commerce().price(10.0, 999.99)));
            p.setStock(random.nextInt(1000));
            p.setImages(List.of("https://picsum.photos/seed/" + i + "/600/400"));
            productService.createProduct(ProductMapper.getProductDto(p));
        }

        System.out.println("✅ Seeded " + PRODUCT_COUNT + " products successfully!");
    }

    public List<String> getCategoriesList() {
         PagedResponse pagedResponse = categoryClient.getAllCategories();
         List<Map<String, Object>> items =  pagedResponse.getItems();
        return items.stream()
                .map(item -> item.get("id").toString())
                .toList();
    }

}
