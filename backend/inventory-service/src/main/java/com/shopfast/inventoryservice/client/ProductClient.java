package com.shopfast.inventoryservice.client;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.common.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        log.debug("Fetching product details for productId: {}", productId);
        try {
            String url = productServiceUrl + "/api/v1/product/" + productId;
            ResponseEntity<ProductDto> response = restTemplate.getForEntity(url, ProductDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching product details for productId: {}", productId, e);
            return null;
        }
    }

    /**
     * Resolves many products in a single request.
     *
     * <p>Replaces per-row {@link #getProductById} calls, which turned a page of N inventory
     * rows into N remote round trips (an N+1 across the network). Falls back to individual
     * lookups only if the batch endpoint is unavailable, so this stays safe during rollout.</p>
     */
    public Map<UUID, ProductDto> getProductsByIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = productIds.stream().distinct().toList();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(productServiceUrl + "/api/v1/product/batch")
                    .queryParam("ids", distinctIds)
                    .build()
                    .toUriString();
            ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductDto>>() {
                    });
            List<ProductDto> products = response.getBody();
            if (products != null) {
                return products.stream()
                        .filter(p -> p != null && p.getId() != null)
                        .collect(Collectors.toMap(ProductDto::getId, Function.identity(), (a, b) -> a));
            }
        } catch (Exception e) {
            log.warn("Batch product lookup failed ({}), falling back to individual lookups", e.getMessage());
        }

        return distinctIds.stream()
                .map(this::getProductById)
                .filter(p -> p != null && p.getId() != null)
                .collect(Collectors.toMap(ProductDto::getId, Function.identity(), (a, b) -> a));
    }
}
