package com.shopfast.productservice;

import com.shopfast.productservice.config.AuditorConfig;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA repository integration test backed by a real PostgreSQL Testcontainer.
 *
 * <p>Skips cleanly (rather than failing) on machines without a Docker daemon.
 */
@DataJpaTest
@Import(AuditorConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@EnabledIf("dockerAvailable")
class ProductRepositoryIntegrationTest {

    static boolean dockerAvailable() {
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return false;
            }
            String apiVersion = DockerClientFactory.instance().client().versionCmd().exec().getApiVersion();
            String[] parts = apiVersion.replace("v", "").split("\\.");
            int minor = Integer.parseInt(parts.length > 1 ? parts[1] : parts[0]);
            return minor >= 40;
        } catch (Exception e) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepository repository;

    private Product sample() {
        return Product.builder()
                .slug("test-product")
                .name("Test Product")
                .description("A sample product")
                .categoryId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(99.99))
                .currency("USD")
                .stock(10)
                .images(List.of("http://img/1.png"))
                .build();
    }

    @Test
    void saveAndFindProduct() {
        Product saved = repository.save(sample());
        assertThat(saved.getId()).isNotNull();

        Product found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Test Product");
    }

    @Test
    void findBySlugReturnsSavedProduct() {
        Product saved = repository.save(sample());

        Optional<Product> bySlug = repository.findBySlug(saved.getSlug());
        assertThat(bySlug).isPresent();
        assertThat(bySlug.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByCategoryIdIsPageable() {
        String category = UUID.randomUUID().toString();
        Product p = sample();
        p.setCategoryId(category);
        repository.save(p);

        var page = repository.findByCategoryId(category, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByNameContainingIgnoreCaseIsCaseInsensitive() {
        Product p = sample();
        p.setName("UniqueWidget");
        repository.save(p);

        List<Product> found = repository.findByNameContainingIgnoreCase("uniquewidget");
        assertThat(found).isNotEmpty();
    }

    @Test
    void findByIdWithImagesLoadsCollection() {
        Product saved = repository.save(sample());

        Optional<Product> found = repository.findByIdWithImages(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getImages()).containsExactly("http://img/1.png");
    }
}
