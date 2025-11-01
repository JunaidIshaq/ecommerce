package com.shopfast.userservice.client;

import com.shopfast.userservice.dto.PagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

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
}
