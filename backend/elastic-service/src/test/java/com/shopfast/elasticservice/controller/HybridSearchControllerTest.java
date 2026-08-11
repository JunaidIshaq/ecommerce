package com.shopfast.elasticservice.controller;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.dto.HybridSearchRequestDto;
import com.shopfast.elasticservice.service.HybridSearchService;
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

@WebMvcTest(controllers = HybridSearchController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class HybridSearchControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private com.shopfast.elasticservice.security.JwtUtils jwtUtils;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private HybridSearchService hybridSearchService;

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
    void hybridSearch_returnsOk() throws Exception {
        HybridSearchRequestDto req = new HybridSearchRequestDto();
        req.setQuery("shoes");
        req.setCategory("footwear");
        req.setPageNumber(0);
        req.setPageSize(10);

        when(hybridSearchService.search(org.mockito.ArgumentMatchers.any(HybridSearchRequestDto.class)))
                .thenReturn(List.of(sampleDocument()));

        mockMvc.perform(post("/api/v1/search/product/hybrid")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }
}
