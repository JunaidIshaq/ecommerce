package com.shopfast.elasticservice.controller;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.service.ProductIndexService;
import com.shopfast.elasticservice.service.ProductSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.elasticservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductSearchController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class ProductSearchControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.elasticservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ProductIndexService indexService;
    @MockBean private ProductSearchService searchService;

    private ProductDocument sampleDocument() {
        return ProductDocument.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Product")
                .description("A test product")
                .category("electronics")
                .brand("brand-x")
                .price(9.99)
                .tags(List.of("tag1"))
                .build();
    }

    @Test
    void index_returnsOk() throws Exception {
        ProductDocument doc = sampleDocument();
        when(indexService.index(org.mockito.ArgumentMatchers.any(ProductDocument.class))).thenReturn(doc);

        mockMvc.perform(post("/api/v1/search/product/index")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(doc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void semanticSearch_returnsOk() throws Exception {
        when(searchService.semanticSearch("shoes", 10)).thenReturn(List.of(sampleDocument()));

        mockMvc.perform(get("/api/v1/search/product/semantic")
                        .param("q", "shoes")
                        .param("k", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    void semanticSearch_withDefaultK_returnsOk() throws Exception {
        when(searchService.semanticSearch("shoes", 10)).thenReturn(List.of(sampleDocument()));

        mockMvc.perform(get("/api/v1/search/product/semantic")
                        .param("q", "shoes"))
                .andExpect(status().isOk());
    }
}
