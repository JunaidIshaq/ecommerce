package com.shopfast.productservice.controller;

import com.shopfast.productservice.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.service.ProductService;
import com.shopfast.productservice.util.MapperUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

@Tag(name = "Products", description = "Product CRUD and Search APIs")
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private ProductService productService;

    @Operation(summary = "Create a new product", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDto productDto) throws IOException {
        return ResponseEntity.ok(productService.createProduct(productDto));
    }


    @Operation(summary = "Get all products (paginated)")
    @GetMapping
    public ResponseEntity<PagedResponse<ProductDto>> getAllProducts(
            @RequestParam(name = "pageNumber", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int size
    ) {
        PagedResponse<Product> pagedProducts = productService.getAllProducts(page, size);
        List<ProductDto> productDtos = pagedProducts.getItems()
                .stream()
                .map(MapperUtils::getProductDto)
                .toList();

        PagedResponse<ProductDto> response = new PagedResponse<>(
                productDtos,
                pagedProducts.getTotalItems(),
                pagedProducts.getTotalPages(),
                page,
                size
        );
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto productDto) throws IOException {
        Product updated = productService.updateProduct(id, MapperUtils.getProduct(productDto));
        return ResponseEntity.ok(MapperUtils.getProductDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Search products with filters")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductDto>> searchProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categories", required = false) List<String> categories,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = "desc") String sortOrder,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        PagedResponse<Product> pagedResults = productService.searchProducts(
                keyword, categories, minPrice, maxPrice, sortBy, sortOrder, page, size
        );

        List<ProductDto> dtoList = pagedResults.getItems().stream()
                .map(MapperUtils::getProductDto)
                .toList();

        return ResponseEntity.ok(
                new PagedResponse<>(dtoList, pagedResults.getTotalItems(), pagedResults.getTotalPages(), page, size)
        );
    }



}
