package com.shopfast.orderservice.client;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.orderservice.dto.ProductDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only view of product-service.
 *
 * <p>{@code product.service.url} already points at the {@code /api/v1/product}
 * root, so paths appended here must be relative to it. The previous code
 * re-appended the whole prefix, producing {@code /api/v1/product/api/v1/product/{id}},
 * which 404'd on every call and made product enrichment silently return null.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8080/api/v1/product}")
    private String productServiceUrl;

    public List<String> fetchAllProducts() {
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(
                productServiceUrl + "/ids?pageNumber=1&pageSize=1000", PagedResponse.class);
        return Objects.requireNonNull(response.getBody()).getItems();
    }

    public ProductDetailDto fetchProductById(UUID productId) {
        try {
            ResponseEntity<ProductDetailDto> response = restTemplate.getForEntity(
                    productServiceUrl + "/" + productId, ProductDetailDto.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product {} not found in product-service", productId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching product {} from product-service: {}", productId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches many products in one round trip.
     *
     * <p>Rendering an order previously issued one HTTP call per line item, so a
     * 20-item order meant 20 sequential requests. Callers should use this and
     * index the result instead.</p>
     *
     * @return products keyed by id; ids that could not be resolved are absent
     *         rather than mapped to null, so callers can use a plain lookup.
     */
    public Map<UUID, ProductDetailDto> fetchProductsByIds(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> distinctIds = productIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String ids = distinctIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        try {
            ResponseEntity<List<ProductDetailDto>> response = restTemplate.exchange(
                    productServiceUrl + "/batch?ids=" + ids,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ProductDetailDto>>() {});

            List<ProductDetailDto> products = response.getBody();
            if (products == null) {
                return Collections.emptyMap();
            }

            Map<UUID, ProductDetailDto> byId = new LinkedHashMap<>();
            for (ProductDetailDto product : products) {
                if (product != null && product.getId() != null) {
                    byId.put(product.getId(), product);
                }
            }
            return byId;

        } catch (Exception e) {
            // Enrichment is cosmetic: the order itself is already correct, so a
            // product-service outage degrades to ids-only rather than a 500.
            log.error("Batch product lookup failed for {} ids: {}", distinctIds.size(), e.getMessage());
            return Collections.emptyMap();
        }
    }
}
