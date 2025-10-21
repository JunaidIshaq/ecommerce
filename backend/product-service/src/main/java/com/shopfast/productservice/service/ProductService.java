package com.shopfast.productservice.service;

import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.productservice.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.SearchResult;
import com.shopfast.productservice.exception.InvalidCategoryException;
import com.shopfast.productservice.exception.ProductNotFoundException;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.search.ElasticProductSearchService;
import com.shopfast.productservice.util.MapperUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProductService {

    private final ElasticProductSearchService elasticProductSearchService;

    private ProductRepository productRepository;

    private final CategoryClient categoryClient;


    public ProductService(ProductRepository repo, ElasticProductSearchService searchService, ProductRepository productRepository, CategoryClient categoryClient) {
        this.elasticProductSearchService = searchService;
        this.productRepository = productRepository;
        this.categoryClient = categoryClient;
    }


    public Optional<ProductDto> findBySlug(String slug) {
        return productRepository.findBySlug(slug).map(this::toDto);
    }


    private ProductDto toDto(Product p) {
        ProductDto d = new ProductDto();
        d.id = p.getId();
        d.slug = p.getSlug();
        d.name = p.getName();
        d.description = p.getDescription();
        d.categoryId = p.getCategoryId();
        d.price = p.getPrice();
        d.currency = p.getCurrency();
        d.stock = p.getStock();
        d.rating = p.getRating();
        d.images = p.getImages();
        d.tags = p.getTags();
        return d;
    }

    public Product createProduct(@Valid ProductDto productDto) throws IOException, InvalidCategoryException {
        Product product = MapperUtils.createProduct(productDto);
        if (!categoryClient.validateCategoryExists(product.getCategoryId())) {
            throw new InvalidCategoryException(product.getCategoryId());
        }
        try {
            Product saved = productRepository.save(product);
            elasticProductSearchService.indexProduct(saved);
        }catch (Exception e){
            e.printStackTrace();  // or use log.error("Elasticsearch indexing failed", e);
            throw new RuntimeException("Failed to index product: " + e.getMessage(), e);
        }
        return product;
    }

    public PagedResponse<Product> getAllProducts(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);
        
        return new PagedResponse<>(
                productPage.getContent(), 
                productPage.getTotalElements(), 
                productPage.getTotalPages(),
                page,
                size);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product updateProduct(String id, Product updatedProduct) throws InvalidCategoryException {
        if (!categoryClient.validateCategoryExists(updatedProduct.getCategoryId())) {
            throw new InvalidCategoryException(updatedProduct.getCategoryId());
        }
        return productRepository.findById(id).map(existing -> {
            existing.setName(updatedProduct.getName());
            existing.setDescription(updatedProduct.getDescription());
            existing.setCategoryId(updatedProduct.getCategoryId());
            existing.setPrice(updatedProduct.getPrice());
            existing.setImages(updatedProduct.getImages());
            existing.setStock(updatedProduct.getStock());
            Product saved = productRepository.save(existing);
            try {
                elasticProductSearchService.indexProduct(saved);
            } catch (IOException e) {
                throw new RuntimeException("Failed to reindex product", e);
            }
            return saved;
        }).orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        elasticProductSearchService.deleteProductFromIndex(id);
    }

    public List<Product> searchByTitle(String title) {
        return productRepository.findByNameContainingIgnoreCase(title);
    }

    public PagedResponse<Product> searchProducts(
            String keyword,
            List<String> categoryIds,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String sortOrder,
            int page,
            int size
    ) {
        try {
            SearchResult result = elasticProductSearchService.searchProducts(
                    keyword, categoryIds, minPrice, maxPrice, sortBy, sortOrder, page, size
            );

            long totalItems = result.getTotalHits();
            int totalPages = (int) Math.ceil((double) totalItems / size);

            return new PagedResponse<>(
                    result.getProducts(),
                    totalItems,
                    totalPages,
                    page,
                    size
            );
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

}
