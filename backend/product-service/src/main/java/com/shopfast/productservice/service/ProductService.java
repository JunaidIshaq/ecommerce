package com.shopfast.productservice.service;

import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.search.ElasticProductSearchService;
import lombok.extern.slf4j.Slf4j;
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

    public Product createProduct(Product product) throws IOException {
        Product saved = productRepository.save(product);
        elasticProductSearchService.indexProduct(saved);
        return saved;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
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
                throw new RuntimeException(e);
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        elasticProductSearchService.deleteProductFromIndex(id);
    }

    public List<Product> searchByTitle(String title) {
        return productRepository.findByNameContainingIgnoreCase(title);
    }

    public List<Product> searchProducts(
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
            return elasticProductSearchService.searchProducts(
                    keyword, categoryIds, minPrice, maxPrice, sortBy, sortOrder, page, size);
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

}
