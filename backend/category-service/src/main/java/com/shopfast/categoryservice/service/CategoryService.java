package com.shopfast.categoryservice.service;

import com.shopfast.categoryservice.exception.CategoryNotFoundException;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
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

    public Category createCategory(Category category) {
        log.info("Creating new category {}", category.getName());

        // Prevent Duplicates
        categoryRepository.findByNameIgnoreCase(category.getName())
                .ifPresent(existingCategory -> {
                    throw new IllegalStateException("Category already exists");
                });
        return categoryRepository.save(category);
    }

    public Page<Category> getAllCategories(int pageNumber, int pageSize) {
        log.info("Getting all categories");
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return categoryRepository.findAll(pageable);
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public List<Category> getSubCategories(String parentId) {
        log.info("Getting subcategories for category {}", parentId);
        return categoryRepository.findByParentId(parentId);
    }

    public Category updateCategory(String id, Category updatedCategory) {
        log.info("Updating category with id : {} and updatedCategory : {}", id, updatedCategory.getName());
        Category existingCategory = getCategoryById(id);
        existingCategory.setName(updatedCategory.getName());
        existingCategory.setDescription(updatedCategory.getDescription());
        existingCategory.setParentId(updatedCategory.getParentId());
        existingCategory.setSubCategoryIds(updatedCategory.getSubCategoryIds());

        return categoryRepository.save(existingCategory);
    }
    
    public void deleteCategory(String id) {
        if(!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }
}
