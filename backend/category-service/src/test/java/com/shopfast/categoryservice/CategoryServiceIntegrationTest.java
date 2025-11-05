//package com.shopfast.categoryservice;
//
//import com.shopfast.categoryservice.model.Category;
//import com.shopfast.categoryservice.repository.CategoryRepository;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//@Testcontainers
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//class CategoryServiceIntegrationTest {
//
//    @Container
//    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
//
//    @DynamicPropertySource
//    static void mongoProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
//    }
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Test
//    @Order(1)
//    void createAndFindCategory() {
//        Category saved = categoryRepository.save(Category.builder()
//                .name("Electronics")
//                .description("Electronic items")
//                .build());
//
//        assertThat(saved.getId()).isNotNull();
//        List<Category> all = categoryRepository.findAll();
//        assertThat(all).hasSize(1);
//    }
//
//    @Test
//    @Order(2)
//    void findByNameIgnoreCase() {
//        var found = categoryRepository.findByNameIgnoreCase("electronics");
//        assertThat(found).isPresent();
//    }
//
//}
