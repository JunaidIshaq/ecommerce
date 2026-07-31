package com.shopfast.inventoryservice.client;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.common.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    public ProductClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getAllProductIdsUrl = "http://localhost:8080/api/v1/product/ids?pageNumber=1&pageSize=1000";

    public List fetchAllProducts() {
        log.info("Fetching all products from Product Service...");
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(getAllProductIdsUrl, PagedResponse.class);
         return Objects.requireNonNull(response.getBody()).getItems();
    }

    public ProductDto getProductById(UUID productId) {
        log.info("Fetching product details for productId: {}", productId);
        try {
            String url = productServiceUrl + "/api/v1/product/" + productId.toString();
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching product details for productId: {}", productId, e);
            return null;
        }
    }
}
