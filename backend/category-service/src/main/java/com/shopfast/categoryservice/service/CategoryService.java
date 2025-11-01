package com.shopfast.categoryservice.service;

import com.shopfast.categoryservice.dto.CategoryDto;
import com.shopfast.categoryservice.dto.PagedResponse;
import com.shopfast.categoryservice.exception.CategoryNotFoundException;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import com.shopfast.categoryservice.search.ElasticCategorySearchService;
import com.shopfast.categoryservice.util.CategoryMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ElasticCategorySearchService elasticService;

    public CategoryService(CategoryRepository categoryRepository, ElasticCategorySearchService elasticService) {
        this.categoryRepository = categoryRepository;
        this.elasticService = elasticService;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#result.id", condition = "#result != null")
    })
    public Category createCategory(Category category) throws IOException {
        log.info("Creating new category {}", category.getName());

        // Prevent Duplicates
        categoryRepository.findByNameIgnoreCase(category.getName())
                .ifPresent(existingCategory -> {
                    throw new IllegalArgumentException("Category with this name already exists");
                });
        category = categoryRepository.save(category);
        elasticService.indexCategory(category);
        return category;
    }

    @Transactional
    @Cacheable(
            value = "category",
            key = "'pageNumber_' + #pageNumber + '_pageSize_' + #pageSize"
    )
    public PagedResponse<CategoryDto> getAllCategories(int pageNumber, int pageSize) {
        log.info("Getting all categories() -> p");
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        List<CategoryDto> categoryDtos = categoryPage.stream().map(CategoryMapper::getCategoryDto).toList();

        return new PagedResponse<>(
                categoryDtos,
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    @Transactional
    @Cacheable(value = "category", key = "#id")
    public Category getCategoryById(String id) {
        return categoryRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    @Cacheable(value = "subcategories", key = "#parentId")
    public List<Category> getSubCategories(String parentId) {
        log.info("Getting subcategories for category {}", parentId);
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "subcategories", allEntries = true)
    })
    public Category updateCategory(String id, Category updatedCategory) throws IOException {
        log.info("Updating category with id : {} and updatedCategory : {}", id, updatedCategory.getName());
        Category existingCategory = getCategoryById(id);
        existingCategory.setName(updatedCategory.getName());
        existingCategory.setDescription(updatedCategory.getDescription());
        existingCategory.setParentId(updatedCategory.getParentId());
        existingCategory.setSubCategoryIds(updatedCategory.getSubCategoryIds());

        existingCategory = categoryRepository.save(existingCategory);
        elasticService.indexCategory(existingCategory);
        return existingCategory;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "subcategories", allEntries = true)
    })
    public void deleteCategory(String id) {
        if(!categoryRepository.existsById(UUID.fromString(id))) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(UUID.fromString(id));
    }
}
