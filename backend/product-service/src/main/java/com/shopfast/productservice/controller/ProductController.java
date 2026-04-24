package com.shopfast.productservice.controller;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.productservice.dto.ProductDto;
import com.shopfast.productservice.dto.ProductInternalResponseDto;
import com.shopfast.productservice.model.Product;
import com.shopfast.productservice.service.ProductService;
import com.shopfast.productservice.util.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "Product CRUD and Search APIs")
@RestController
@RequestMapping("/api/v1/product")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create a new product", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) throws IOException {
        return ResponseEntity.ok(productService.createProduct(productDto));
    }


    @Operation(summary = "Get all products (paginated)")
    @GetMapping
    public ResponseEntity<PagedResponse<ProductDto>> getAllProducts(
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortOrder", required = false) String sortOrder
    ) {
        return ResponseEntity.ok(productService.getAllProducts(
                pageNumber, pageSize, searchKeyword, categoryId, sortBy, sortOrder));
    }

    @Operation(summary = "Get all products (paginated)")
    @GetMapping("/ids")
    public ResponseEntity<PagedResponse<UUID>> getAllProductIds(
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(productService.getAllProductIds(pageNumber, pageSize));
    }

    @Operation(summary = "Get all products for admin (paginated)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin")
    public ResponseEntity<PagedResponse<ProductDto>> getAllProductsAdmin(
            @PathVariable(name = "pageNumber", required = false) Integer pageNumber,
            @PathVariable(name = "pageSize", required = false) Integer pageSize
    ) {
        int pn = pageNumber != null ? pageNumber : 1;
        int ps = pageSize != null ? pageSize : 10;
        return ResponseEntity.ok(productService.getAllProducts(pn, ps));
    }

    @Operation(summary = "Get Product details based on Id")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Get Product details based on Id (internal)")
    @GetMapping("/{id}/internal")
    public ResponseEntity<ProductInternalResponseDto> getProductByIdInternal(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.getProductByIdInternal(id));
    }

    @Operation(summary = "Update Product based on Id", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable("id") String id, 
                                                     @Valid @RequestBody ProductDto productDto) throws IOException {
        return ResponseEntity.ok(productService.updateProduct(id, ProductMapper.getProduct(productDto)));
    }

    @Operation(summary = "Delete Product based on Id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Search products with filters")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<Product>> searchProducts(
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

        return ResponseEntity.ok(pagedResults);
    }

}
