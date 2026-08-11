package com.shopfast.productservice.controller;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.ProductInternalResponseDto;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.productservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;
    @MockBean private com.shopfast.productservice.security.JwtUtils jwtUtils;

    private ProductDto sampleDto() {
        return ProductDto.builder()
                .id(UUID.randomUUID())
                .slug("test-product")
                .name("Test Product")
                .description("A sample product description")
                .categoryId(UUID.randomUUID().toString())
                .price(BigDecimal.valueOf(49.99))
                .currency("USD")
                .stock(5)
                .build();
    }

    @Test
    void getAllProductsReturnsPagedContent() throws Exception {
        PagedResponse<ProductDto> page = new PagedResponse<>(List.of(sampleDto()), 1, 1, 0, 10);
        when(productService.getAllProducts(any(Integer.class), any(Integer.class), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getProductByIdReturnsDto() throws Exception {
        when(productService.getProductById(anyString())).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/product/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void getProductByIdInternalReturnsInternalDto() throws Exception {
        ProductInternalResponseDto internal = new ProductInternalResponseDto();
        when(productService.getProductByIdInternal(anyString())).thenReturn(internal);

        mockMvc.perform(get("/api/v1/product/{id}/internal", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getProductsByIdsReturnsList() throws Exception {
        when(productService.getProductsByIds(anyList())).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/v1/product/batch").param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    void createProductReturnsCreatedDto() throws Exception {
        when(productService.createProduct(any(ProductDto.class))).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/product")
                        .contentType("application/json")
                        .content("""
                                {"slug":"test-product","name":"Test Product",
                                 "description":"A sample product description",
                                 "categoryId":"%s","price":49.99,"currency":"USD","stock":5}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void createProductRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/product")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProductReturnsUpdatedDto() throws Exception {
        when(productService.updateProduct(anyString(), any(Product.class))).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/product/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"slug":"test-product","name":"Test Product",
                                 "description":"A sample product description",
                                 "categoryId":"%s","price":49.99,"currency":"USD","stock":5}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/product/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchProductsReturnsPagedResults() throws Exception {
        PagedResponse<Product> page = new PagedResponse<>(List.of(new Product()), 1, 1, 0, 10);
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/product/search").param("keyword", "widget"))
                .andExpect(status().isOk());
    }
}
