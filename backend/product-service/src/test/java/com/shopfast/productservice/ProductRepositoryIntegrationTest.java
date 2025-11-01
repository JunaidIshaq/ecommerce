//package com.shopfast.productservice;
//
//import org.springframework.boot.test.context.SpringBootTest;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@SpringBootTest
//@Testcontainers
//public class ProductRepositoryIntegrationTest {
//
//
//    @Autowired
//    private ProductRepository repository;
//
//    @Container
//    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
//
//    @DynamicPropertySource
//    static void mongoProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
//    }
//
//    @Test
//    void saveAndFindProduct() {
//        Product p = new Product();
//        p.setName("Test Product");
//        p.setPrice(BigDecimal.valueOf(99.99));
//        Product saved = repository.save(p);
//        assertThat(saved.getId()).isNotNull();
//
//        Product found = repository.findById(saved.getId()).orElseThrow();
//        assertThat(found.getName()).isEqualTo("Test Product");
//    }
//}
