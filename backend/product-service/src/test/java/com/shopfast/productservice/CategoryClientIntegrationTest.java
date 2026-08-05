package com.shopfast.productservice;

import com.shopfast.productservice.client.CategoryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requires a working Docker daemon.
 *
 * <p>The class-level condition skips rather than fails on machines without Docker.
 * This test never actually ran before - the build was using surefire 2.12.4, which
 * cannot see JUnit 5 tests - so it only started erroring once the plugin was
 * upgraded. Skipping keeps `mvn test` usable locally while the test still runs in
 * CI, where Docker is present.
 */
@SpringBootTest
@Testcontainers
@EnabledIf("dockerAvailable")
class CategoryClientIntegrationTest {

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

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
