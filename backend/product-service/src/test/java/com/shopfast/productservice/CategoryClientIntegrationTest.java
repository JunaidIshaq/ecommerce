package com.shopfast.productservice;

import com.shopfast.productservice.client.CategoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CategoryClientIntegrationTest {

    @Autowired
    private CategoryClient categoryClient;

    @Container
    static GenericContainer<?> categoryService =
            new GenericContainer<>("junaidishaq/category-service:latest")
                    .withExposedPorts(8082);

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String baseUrl = "http://" + categoryService.getHost() + ":" + categoryService.getMappedPort(8082)
                + "/api/v1/category";
        registry.add("category.service.url", () -> baseUrl);
    }

    @Test
    void validateCategoryExistsShouldReturnFalseForUnknown() {
        boolean exists = categoryClient.validateCategoryExists("nonexistent");
        assertThat(exists).isFalse();
    }

}
