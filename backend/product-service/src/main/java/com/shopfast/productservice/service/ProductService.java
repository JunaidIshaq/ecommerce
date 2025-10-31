package com.shopfast.productservice.service;

import com.shopfast.common.events.ProductEvent;
import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.productservice.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.SearchResult;
import com.shopfast.productservice.events.KafkaProductProducer;
import com.shopfast.productservice.exception.InvalidCategoryException;
import com.shopfast.productservice.exception.ProductNotFoundException;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.search.ElasticProductSearchService;
import com.shopfast.productservice.util.ProductMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private ElasticProductSearchService elasticProductSearchService;

    private ProductRepository productRepository;

    private CategoryClient categoryClient;

    private KafkaProductProducer kafkaProductProducer;

    public ProductService(ElasticProductSearchService elasticProductSearchService, ProductRepository productRepository, CategoryClient categoryClient, KafkaProductProducer kafkaProductProducer) {
        this.elasticProductSearchService = elasticProductSearchService;
        this.productRepository = productRepository;
        this.categoryClient = categoryClient;
        this.kafkaProductProducer = kafkaProductProducer;
    }

    public Optional<ProductDto> findBySlug(String slug) {
        return productRepository.findBySlug(slug).map(this::toDto);
    }


    private ProductDto toDto(Product p) {
        ProductDto d = new ProductDto();
        d.id = p.getId().toString();
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

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#result.id", condition = "#result != null"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public ProductDto createProduct(@Valid ProductDto productDto) throws IOException, InvalidCategoryException {
        Product product = ProductMapper.createProduct(productDto);
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
        // Publish product created event
        ProductEvent event = new ProductEvent(
                java.util.UUID.randomUUID().toString(),
                "PRODUCT_CREATED",
                1,
                java.time.Instant.now(),
                Map.of(
                        "productId", product.getId(),
                        "name", product.getName(),
                        "categoryId", product.getCategoryId(),
                        "price", product.getPrice()
                )
        );

        kafkaProductProducer.publishProductEvent(event);
        log.info("✅ Product event published for {}", product.getId());
        return ProductMapper.getProductDto(product);
    }

    @Cacheable(
            value = "productsPage",
            key = "'pageNumber_' + #pageNumber + '_pageSize_' + #pageSize"
    )
    public PagedResponse<ProductDto> getAllProducts(int pageNumber, int pageSize) {
        log.info("🧠 Inside getAllProducts() -> pageNumber={}, pageSize={}", pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductDto> productDtos = productPage.getContent()
                .stream()
                .map(ProductMapper::getProductDto)
                .toList();

        return new PagedResponse<>(
                productDtos,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    public PagedResponse<UUID> getAllProductIds(int pageNumber, int pageSize) {
        log.info("🧠 Inside getAllProductIds() -> pageNumber={}, pageSize={}", pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);

        List<UUID> productDtos = productPage.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        return new PagedResponse<>(
                productDtos,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }


    @Cacheable(value = "product", key = "#id")
    public ProductDto getProductById(String id) {
        Optional<Product> product = productRepository.findById(UUID.fromString(id));
        return product.map(ProductMapper::getProductDto).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public ProductDto updateProduct(String id, Product updatedProduct) throws InvalidCategoryException {
        if (!categoryClient.validateCategoryExists(updatedProduct.getCategoryId())) {
            throw new InvalidCategoryException(updatedProduct.getCategoryId());
        }
        return productRepository.findById(UUID.fromString(id)).map(existing -> {
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
            return ProductMapper.getProductDto(saved);
        }).orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public void deleteProduct(String id) {
        productRepository.deleteById(UUID.fromString(id));
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


            PagedResponse<Product> pagedResults = new PagedResponse<>(
                    result.getProducts(),
                    totalItems,
                    totalPages,
                    page,
                    size
            );

            return pagedResults;
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    /**
     * Updates inStock status of a product, persists it, and reindexes in Elasticsearch.
     */
    public void updateStockAndAvailability(String productId, int stock) throws IOException {
        log.info("Updating stock for product {} -> {}", productId, stock);

        // 1️⃣ Find product in MongoDB
        Optional<Product> optionalProduct = productRepository.findById(UUID.fromString(productId));
        if (optionalProduct.isEmpty()) {
            log.warn("Product {} not found, skipping stock update", productId);
            return;
        }

            Product product = optionalProduct.get();

            product.setStock(stock);
            productRepository.save(product);

            log.info("Updated product {} in MongoDB. New stock = {}", productId, stock);

            // 3️⃣ Reindex product in Elasticsearch (try-catch to avoid breaking Kafka listener)
            try {
                elasticProductSearchService.indexProduct(product);
                log.info("Reindexed product {} in Elasticsearch", productId);
            } catch (IOException e) {
                log.error("Failed to reindex product {} in Elasticsearch: {}", productId, e.getMessage(), e);
            }
    }
}
