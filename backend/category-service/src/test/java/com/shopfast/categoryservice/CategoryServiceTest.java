package com.shopfast.categoryservice.service;

import com.shopfast.categoryservice.exception.CategoryNotFoundException;
import com.shopfast.categoryservice.model.Category;
import com.shopfast.categoryservice.repository.CategoryRepository;
import com.shopfast.categoryservice.search.ElasticCategorySearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ElasticCategorySearchService elasticService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategorySavesWhenNameIsUnique() throws Exception {
        Category category = Category.builder().name("Books").description("d").build();
        when(categoryRepository.findByNameIgnoreCase("Books")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category saved = categoryService.createCategory(category);

        assertThat(saved.getName()).isEqualTo("Books");
        verify(categoryRepository).save(category);
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        Category category = Category.builder().name("Books").build();
        when(categoryRepository.findByNameIgnoreCase("Books"))
                .thenReturn(Optional.of(Category.builder().name("Books").build()));

        assertThatThrownBy(() -> categoryService.createCategory(category))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getCategoryByIdReturnsEntity() {
        UUID id = UUID.randomUUID();
        Category category = Category.builder().id(id).name("Toys").build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(id.toString());

        assertThat(result.getName()).isEqualTo("Toys");
    }

    @Test
    void getCategoryByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(id.toString()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void getSubCategoriesDelegatesToRepository() {
        String parentId = "parent";
        when(categoryRepository.findByParentId(parentId))
                .thenReturn(List.of(Category.builder().name("Child").build()));

        assertThat(categoryService.getSubCategories(parentId)).hasSize(1);
    }

    @Test
    void updateCategoryCopiesFieldsAndSaves() throws Exception {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder().id(id).name("Old").description("old").build();
        Category incoming = Category.builder().name("New").description("new").build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category updated = categoryService.updateCategory(id.toString(), incoming);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("new");
        verify(categoryRepository).save(existing);
    }

    @Test
    void deleteCategoryDeletesWhenPresent() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(true);

        categoryService.deleteCategory(id.toString());

        verify(categoryRepository).deleteById(id);
    }

    @Test
    void deleteCategoryThrowsWhenAbsent() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(id.toString()))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).deleteById(any());
    }
}
