package com.shopfast.categoryservice.controller;

import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.service.CategoryService;
import com.shopfast.common.dto.CategoryDto;
import com.shopfast.common.dto.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.shopfast.categoryservice.security.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;
    @MockBean private com.shopfast.categoryservice.security.JwtUtils jwtUtils;

    private CategoryDto sampleDto() {
        return CategoryDto.builder()
                .id(UUID.randomUUID().toString())
                .name("Electronics")
                .description("Electronic items")
                .build();
    }

    private Category sampleCategory() {
        return Category.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .description("Electronic items")
                .build();
    }

    @Test
    void createCategory_returnsOk() throws Exception {
        Category saved = sampleCategory();
        when(categoryService.createCategory(any(Category.class))).thenReturn(saved);

        CategoryDto request = CategoryDto.builder()
                .id(UUID.randomUUID().toString())
                .name("Electronics")
                .description("Electronic items")
                .build();

        mockMvc.perform(post("/api/v1/category")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void createCategory_invalidBody_returnsBadRequest() throws Exception {
        CategoryDto request = CategoryDto.builder()
                .id(UUID.randomUUID().toString())
                .name("E")
                .description("Electronic items")
                .build();

        mockMvc.perform(post("/api/v1/category")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCategories_returnsOk() throws Exception {
        PagedResponse<CategoryDto> page = new PagedResponse<>(
                List.of(sampleDto()), 1, 1, 1, 10);
        when(categoryService.getAllCategories(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/category").param("pageNumber", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getCategoryById_returnsOk() throws Exception {
        Category category = sampleCategory();
        when(categoryService.getCategoryById(anyString())).thenReturn(category);

        mockMvc.perform(get("/api/v1/category/{id}", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getSubCategories_returnsOk() throws Exception {
        when(categoryService.getSubCategories(anyString())).thenReturn(List.of(sampleCategory()));

        mockMvc.perform(get("/api/v1/category/{parentId}/subcategories", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void updateCategory_returnsOk() throws Exception {
        Category updated = sampleCategory();
        when(categoryService.updateCategory(anyString(), any(Category.class))).thenReturn(updated);

        CategoryDto request = CategoryDto.builder()
                .id(updated.getId().toString())
                .name("Electronics")
                .description("Updated desc")
                .build();

        mockMvc.perform(put("/api/v1/category/{id}", updated.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void updateCategory_invalidBody_returnsBadRequest() throws Exception {
        CategoryDto request = CategoryDto.builder()
                .id(UUID.randomUUID().toString())
                .name("")
                .build();

        mockMvc.perform(put("/api/v1/category/{id}", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/category/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }
}
