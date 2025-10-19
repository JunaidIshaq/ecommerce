package com.shopfast.productservice.service;

import com.shopfast.productservice.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.SearchResult;
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

    private final ProductRepository repo;

    private final ElasticProductSearchService elasticProductSearchService;

    private ProductRepository productRepository;

    public ProductService(ProductRepository repo, ElasticProductSearchService searchService, ProductRepository productRepository) {
        this.repo = repo;
        this.elasticProductSearchService = searchService;
        this.productRepository = productRepository;
    }


    public Optional<ProductDto> findBySlug(String slug) {
        return repo.findBySlug(slug).map(this::toDto);
    }


    private ProductDto toDto(Product p) {
        ProductDto d = new ProductDto();
        d.id = p.getId();
        d.slug = p.getSlug();
        d.name = p.getName();
        d.description = p.getDescription();
        d.category = p.getCategory();
        d.price = p.getPrice();
        d.currency = p.getCurrency();
        d.stock = p.getStock();
        d.rating = p.getRating();
        d.images = p.getImages();
        d.tags = p.getTags();
        return d;
    }

    public Product createProduct(@Valid ProductDto productDto) throws IOException {
        Product product = MapperUtils.createProduct(productDto);
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

    public Product updateProduct(String id, Product updatedProduct) {
        return productRepository.findById(id).map(existing -> {
            existing.setName(updatedProduct.getName());
            existing.setDescription(updatedProduct.getDescription());
            existing.setCategory(updatedProduct.getCategory());
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
