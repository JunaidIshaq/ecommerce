package com.shopfast.orderservice.client;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.dto.ProductDetailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class ProductClient {

    @Autowired
    private final RestTemplate restTemplate;

    public ProductClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${product.service.url:http://localhost:8080/api/v1/product}")
    private String productServiceUrl;

    public List<String> fetchAllProducts() {
        log.info("Fetching all products from Product Service...");
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(productServiceUrl + "/ids?pageNumber=1&pageSize=1000", PagedResponse.class);
        return Objects.requireNonNull(response.getBody()).getItems();
    }

    public ProductDetailDto fetchProductById(UUID productId) {
        try {
            log.info("Fetching product {} from Product Service...", productId);
            ResponseEntity<ProductDetailDto> response = restTemplate.getForEntity(
                    productServiceUrl + "/" + productId, ProductDetailDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product {} not found in Product Service", productId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching product {} from Product Service: {}", productId, e.getMessage());
            return null;
        }
    }
}
