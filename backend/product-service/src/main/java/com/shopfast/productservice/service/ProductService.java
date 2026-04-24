package com.shopfast.productservice.service;

import com.shopfast.common.events.ProductEvent;
import com.shopfast.productservice.client.CategoryClient;
import com.shopfast.common.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.ProductInternalResponseDto;
import com.shopfast.productservice.events.KafkaProductProducer;
import com.shopfast.productservice.exception.InvalidCategoryException;
import com.shopfast.productservice.exception.ProductNotFoundException;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.repository.ProductRepository;
import com.shopfast.productservice.util.ProductMapper;
import io.jsonwebtoken.lang.Strings;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private ProductRepository productRepository;

    private CategoryClient categoryClient;

    private KafkaProductProducer kafkaProductProducer;

    // Elasticsearch disabled - removed ElasticProductSearchService dependency
    public ProductService(ProductRepository productRepository, CategoryClient categoryClient, KafkaProductProducer kafkaProductProducer) {
        this.productRepository = productRepository;
        this.categoryClient = categoryClient;
        this.kafkaProductProducer = kafkaProductProducer;
    }

    public Optional<ProductDto> findBySlug(String slug) {
        return productRepository.findBySlug(slug).map(ProductMapper::getProductDto);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#result.id", condition = "#result != null"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public ProductDto createProduct(@Valid ProductDto productDto) throws InvalidCategoryException {
        Product product = ProductMapper.createProduct(productDto);
        if (!categoryClient.validateCategoryExists(product.getCategoryId())) {
            throw new InvalidCategoryException(product.getCategoryId());
        }
        Product saved = productRepository.save(product);
        
        // Publish product created event
        ProductEvent event = new ProductEvent(
                UUID.randomUUID().toString(),
                "PRODUCT_CREATED",
                1,
                Instant.now(),
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

    @Transactional
    @Cacheable(
            value = "productsPage",
            key = "'pageNumber_' + #pageNumber + '_pageSize_' + #pageSize + '_searchKeyword_' + #searchKeyword + '_categoryId_' + #categoryId + '_sortBy_' + #sortBy + '_sortOrder_' + #sortOrder"
    )
    public PagedResponse<ProductDto> getAllProducts(int pageNumber, int pageSize, String searchKeyword, String categoryId, String sortBy, String sortOrder) {
        log.info("Class ProductService method getAllProducts() -> pageNumber : {}, pageSize : {}, categoryId : {}, sortBy : {}, sortOrder : {}", pageNumber, pageSize, categoryId, sortBy, sortOrder);
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Product> productPage;

        // Build sort based on parameters, default to createdAt descending
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Sort sort = defaultSort;

        if (Strings.hasText(sortBy) && Strings.hasText(sortOrder)) {
            Sort.Direction direction = sortOrder.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }
        pageable = pageable.withSort(sort);
        if(Strings.hasText(categoryId) && Strings.hasText(searchKeyword)) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndCategoryId(searchKeyword, categoryId, pageable);
        } else if(Strings.hasText(categoryId)) {
            productPage = productRepository.findByCategoryId(categoryId, pageable);
        } else if(Strings.hasText(searchKeyword)) {
            productPage = productRepository.findByNameContainingIgnoreCase(searchKeyword, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

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

    @Transactional
    public PagedResponse<UUID> getAllProductIds(int pageNumber, int pageSize) {
        log.debug("Fetching all product IDs -> pageNumber={}, pageSize={}", pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);

        List<UUID> productIds = productPage.getContent()
                .stream()
                .map(Product::getId)
                .toList();

        return new PagedResponse<>(
                productIds,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    /**
     * Get all products for admin page (simplified version without filters)
     */
    @Transactional
    public PagedResponse<ProductDto> getAllProducts(int pageNumber, int pageSize) {
        log.debug("Fetching all products for admin -> pageNumber={}, pageSize={}", pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductDto> productPage = productRepository.findAll(pageable).map(ProductMapper::getProductDto);

        return new PagedResponse<>(
                productPage.getContent(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize()
        );
    }

    @Transactional
    @Cacheable(value = "product", key = "#id")
    public ProductDto getProductById(String id) {
        UUID productId = UUID.fromString(id);
        Optional<Product> product = productRepository.findById(productId);
        return product.map(ProductMapper::getProductDto).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    @Cacheable(value = "productInternalSearch", key = "#id")
    public ProductInternalResponseDto getProductByIdInternal(String id) {
        UUID productId = UUID.fromString(id);
        Product product = productRepository.findByIdWithImages(productId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Force Hibernate to initialize lazy-loaded collection
        if (product.getImages() != null) {
            product.getImages().size();
        }

        return ProductMapper.getProductInternalDto(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productInternalSearch", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public ProductDto updateProduct(String id, Product updatedProduct) throws InvalidCategoryException {
        UUID productId = UUID.fromString(id);
        if (!categoryClient.validateCategoryExists(updatedProduct.getCategoryId())) {
            throw new InvalidCategoryException(updatedProduct.getCategoryId());
        }
        return productRepository.findById(productId).map(existing -> {
            existing.setName(updatedProduct.getName());
            existing.setDescription(updatedProduct.getDescription());
            existing.setCategoryId(updatedProduct.getCategoryId());
            existing.setPrice(updatedProduct.getPrice());
            existing.setImages(updatedProduct.getImages());
            existing.setStock(updatedProduct.getStock());
            Product savedProduct = productRepository.save(existing);
//            try {
//                elasticProductSearchService.indexProduct(savedProduct);
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to reindex product", e);
//            }

            // Publish Product Update event to Kafka for elastic search
            ProductEvent event = new ProductEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("PRODUCT_UPDATED");
            event.setEventVersion(1);
            event.setOccurredAt(Instant.now());

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", savedProduct.getId());
            payload.put("name", savedProduct.getName());
            payload.put("description", savedProduct.getDescription());
            payload.put("category", savedProduct.getCategoryId());
//            payload.put("brand", savedProduct.getBrand());
            payload.put("price", savedProduct.getPrice());
            payload.put("tags", savedProduct.getTags());

            event.setPayload(payload);

            kafkaProductProducer.publishProductEvent(event);
            log.info("Product event published for {}", savedProduct.getId());

            return ProductMapper.getProductDto(savedProduct);
        }).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productInternalSearch", key = "#id"),
            @CacheEvict(value = "productsPage", allEntries = true)
    })
    public void deleteProduct(String id) {
        UUID productId = UUID.fromString(id);
        productRepository.deleteById(productId);
//        elasticProductSearchService.deleteProductFromIndex(id);
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
//            SearchResult result = elasticProductSearchService.searchProducts(
//                    keyword, categoryIds, minPrice, maxPrice, sortBy, sortOrder, page, size
//            );

//            long totalItems = result.getTotalHits();
//            int totalPages = (int) Math.ceil((double) totalItems / size);
//
//
//            PagedResponse<Product> pagedResults = new PagedResponse<>(
//                    result.getProducts(),
//                    totalItems,
//                    totalPages,
//                    page,
//                    size
//            );

//            return pagedResults;
//        } catch (IOException e) {
//            throw new RuntimeException("Search failed", e);
//        }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

        /**
     * Updates inStock status of a product, persists it, and reindexes in Elasticsearch.
     */
    @Transactional
    public void updateStockAndAvailability(String productId, int stock) {
        log.info("Updating stock for product {} -> {}", productId, stock);

        // Find product in database
        Optional<Product> optionalProduct = productRepository.findById(UUID.fromString(productId));
        if (optionalProduct.isEmpty()) {
            log.warn("Product {} not found, skipping stock update", productId);
            return;
        }

        Product product = optionalProduct.get();
        product.setStock(stock);
        productRepository.save(product);

        log.info("Updated product {} in database. New stock = {}", productId, stock);
    }
}
