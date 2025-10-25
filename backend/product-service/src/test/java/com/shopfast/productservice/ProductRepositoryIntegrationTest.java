package com.shopfast.productservice;

import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class ProductRepositoryIntegrationTest {


    @Autowired
    private ProductRepository repository;

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Test
    void saveAndFindProduct() {
        Product p = new Product();
        p.setName("Test Product");
        p.setPrice(BigDecimal.valueOf(99.99));
        Product saved = repository.save(p);
        assertThat(saved.getId()).isNotNull();

        Product found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Test Product");
    }
}
