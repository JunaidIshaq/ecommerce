package com.shopfast.categoryservice.controller;

import com.shopfast.categoryservice.dto.PagedResponse;
import com.shopfast.categoryservice.dto.CategoryDto;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.service.CategoryService;
import com.shopfast.categoryservice.util.CategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Categories", description = "CRUD operations for categories")
@RestController
@RequestMapping("/api/v1/category")
public class CategoryController {

    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a new category", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) throws IOException {
       Category saved = categoryService.createCategory(CategoryMapper.getCategory(categoryDto));
        return ResponseEntity.ok(CategoryMapper.getCategoryDto(saved));
    }


    @Operation(summary = "Get all categories")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        List<CategoryDto> response = categoryService.getAllCategories(pageNumber, pageSize)
                .stream().map(CategoryMapper::getCategoryDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get category by Id")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable("id") String id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(CategoryMapper.getCategoryDto(category));
    }

    @Operation(summary = "Get subcategories of a parent category")
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<CategoryDto>> getSubCategories(@PathVariable String parentId) {
        List<CategoryDto> list = categoryService.getSubCategories(parentId)
                .stream().map(CategoryMapper::getCategoryDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Update category details", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable String id, @Valid @RequestBody CategoryDto dto) {
        Category updated = categoryService.updateCategory(id, CategoryMapper.getCategory(dto));
        return ResponseEntity.ok(CategoryMapper.getCategoryDto(updated));
    }


    @Operation(summary = "Delete a category", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }



}
