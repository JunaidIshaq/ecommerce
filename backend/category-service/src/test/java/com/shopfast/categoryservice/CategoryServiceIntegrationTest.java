package com.shopfast.categoryservice;

import com.shopfast.categoryservice.config.AuditorConfig;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class CategoryServiceIntegrationTest {

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
    private CategoryRepository categoryRepository;

    private Category sample(String name) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(name + " description")
                .build();
    }

    @Test
    void createAndFindCategory() {
        Category saved = categoryRepository.save(sample("Electronics"));

        assertThat(saved.getId()).isNotNull();
        List<Category> all = categoryRepository.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    void findByNameIgnoreCaseMatchesRegardlessOfCase() {
        categoryRepository.save(sample("Electronics"));

        Optional<Category> found = categoryRepository.findByNameIgnoreCase("electronics");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
    }

    @Test
    void findByNameIgnoreCaseReturnsEmptyWhenAbsent() {
        Optional<Category> found = categoryRepository.findByNameIgnoreCase("missing");
        assertThat(found).isEmpty();
    }

    @Test
    void findByParentIdReturnsChildren() {
        Category parent = categoryRepository.save(sample("Root"));
        Category child = Category.builder()
                .id(UUID.randomUUID())
                .name("Child")
                .parentId(parent.getId().toString())
                .build();
        categoryRepository.save(child);

        List<Category> children = categoryRepository.findByParentId(parent.getId().toString());
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("Child");
    }
}
