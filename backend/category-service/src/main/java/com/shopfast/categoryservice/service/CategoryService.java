package com.shopfast.categoryservice.service;

import com.shopfast.categoryservice.exception.CategoryNotFoundException;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#result.id", condition = "#result != null")
    })
    public Category createCategory(Category category) {
        log.info("Creating new category {}", category.getName());

        // Prevent Duplicates
        categoryRepository.findByNameIgnoreCase(category.getName())
                .ifPresent(existingCategory -> {
                    throw new IllegalArgumentException("Category with this name already exists");
                });
        return categoryRepository.save(category);
    }

    @Cacheable(value = "category")
    public Page<Category> getAllCategories(int pageNumber, int pageSize) {
        log.info("Getting all categories");
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return categoryRepository.findAll(pageable);
    }

    @Cacheable(value = "category", key = "#id")
    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Cacheable(value = "subcategories", key = "#parentId")
    public List<Category> getSubCategories(String parentId) {
        log.info("Getting subcategories for category {}", parentId);
        return categoryRepository.findByParentId(parentId);
    }

    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "subcategories", allEntries = true)
    })
    public Category updateCategory(String id, Category updatedCategory) {
        log.info("Updating category with id : {} and updatedCategory : {}", id, updatedCategory.getName());
        Category existingCategory = getCategoryById(id);
        existingCategory.setName(updatedCategory.getName());
        existingCategory.setDescription(updatedCategory.getDescription());
        existingCategory.setParentId(updatedCategory.getParentId());
        existingCategory.setSubCategoryIds(updatedCategory.getSubCategoryIds());

        return categoryRepository.save(existingCategory);
    }

    @Caching(evict = {
            @CacheEvict(value = "category", allEntries = true),
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "subcategories", allEntries = true)
    })
    public void deleteCategory(String id) {
        if(!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }
}
