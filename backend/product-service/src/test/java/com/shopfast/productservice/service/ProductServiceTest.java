package com.shopfast.productservice.service;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.common.events.ProductEvent;
import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.exception.InvalidCategoryException;
import com.shopfast.productservice.exception.ProductNotFoundException;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.events.KafkaProductProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private KafkaProductProducer kafkaProductProducer;

    @InjectMocks
    private ProductService productService;

    private ProductDto dto() {
        return ProductDto.builder()
                .id(UUID.randomUUID())
                .name("Widget")
                .categoryId("cat-1")
                .price(new BigDecimal("9.99"))
                .stock(5)
                .build();
    }

    @Test
    void createProductValidatesCategoryAndPublishesEvent() throws Exception {
        ProductDto dto = dto();
        when(categoryClient.validateCategoryExists("cat-1")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product prod = inv.getArgument(0);
            if (prod.getId() == null) prod.setId(java.util.UUID.randomUUID());
            return prod;
        });

        productService.createProduct(dto);

        verify(productRepository).save(any(Product.class));
        verify(kafkaProductProducer).publishProductEvent(any(ProductEvent.class));
    }

    @Test
    void createProductRejectsInvalidCategory() {
        ProductDto dto = dto();
        when(categoryClient.validateCategoryExists("cat-1")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(dto))
                .isInstanceOf(InvalidCategoryException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductByIdReturnsDto() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().id(id).name("Widget").price(new BigDecimal("1.0")).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThat(productService.getProductById(id.toString()).getName()).isEqualTo("Widget");
    }

    @Test
    void getProductByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id.toString()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProductsByIdsReturnsOnlyFound() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(productRepository.findAllById(List.of(id1, id2)))
                .thenReturn(List.of(Product.builder().id(id1).name("A").build())); // id2 not present

        List<ProductDto> result = productService.getProductsByIds(List.of(id1, id2));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id1);
    }

    @Test
    void getProductsByIdsReturnsEmptyForNullOrEmptyInput() {
        assertThat(productService.getProductsByIds(null)).isEmpty();
        assertThat(productService.getProductsByIds(List.of())).isEmpty();
    }

    @Test
    void updateProductValidatesCategoryAndPublishes() throws Exception {
        UUID id = UUID.randomUUID();
        Product existing = Product.builder().id(id).name("Old").categoryId("cat-1").price(new BigDecimal("1.0")).build();
        Product updated = Product.builder().name("New").categoryId("cat-2").price(new BigDecimal("2.0")).build();
        when(categoryClient.validateCategoryExists("cat-2")).thenReturn(true);
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product prod = inv.getArgument(0);
            if (prod.getId() == null) prod.setId(java.util.UUID.randomUUID());
            return prod;
        });

        ProductDto result = productService.updateProduct(id.toString(), updated);

        assertThat(result.getName()).isEqualTo("New");
        verify(kafkaProductProducer).publishProductEvent(any(ProductEvent.class));
    }

    @Test
    void deleteProductDelegatesToRepository() {
        productService.deleteProduct(UUID.randomUUID().toString());
        verify(productRepository).deleteById(any(UUID.class));
    }

    @Test
    void updateStockAndAvailabilitySkipsWhenProductMissing() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        productService.updateStockAndAvailability(id.toString(), 10);

        verify(productRepository, never()).save(any());
    }

    @Test
    void getAllProductsUsesPagingAndMapping() {
        Product p = Product.builder().id(UUID.randomUUID()).name("A").price(new BigDecimal("1.0")).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedResponse<ProductDto> response = productService.getAllProducts(1, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(1);
    }
}
