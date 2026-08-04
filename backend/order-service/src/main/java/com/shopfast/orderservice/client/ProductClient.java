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

    private static final String PRODUCT_API_PATH = "/api/v1/product";

    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://localhost:8080/api/v1/product}")
    private String productServiceUrl;

    /**
     * Returns the product API root, whether or not {@code product.service.url} already
     * includes the {@code /api/v1/product} prefix.
     *
     * <p>The convention is not consistent across the estate: {@code category-service},
     * {@code coupon-service}, {@code payment-service}, {@code review-service} and
     * {@code user-service} configure the URL <em>with</em> the prefix, while
     * {@code order-service}, {@code admin-service}, {@code auth-service},
     * {@code cart-service} and {@code inventory-service} configure it as the bare host.
     * This client appended paths as if the prefix were always present, so in order-service
     * it requested {@code http://product-service:8081/batch}, which product-service answered
     * with "No static resource batch" - surfaced as a 500 and swallowed by the caller, so
     * every order rendered with null product names, descriptions and images.</p>
     *
     * <p>Normalising here rather than editing one YAML line means the client keeps working
     * under either convention, including if {@code PRODUCT_SERVICE_URL} is overridden at
     * deploy time with the other form.</p>
     */
    private String productApiRoot() {
        String base = productServiceUrl == null ? "" : productServiceUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.endsWith(PRODUCT_API_PATH) ? base : base + PRODUCT_API_PATH;
    }

    public List<String> fetchAllProducts() {
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(
                productApiRoot() + "/ids?pageNumber=1&pageSize=1000", PagedResponse.class);
        return Objects.requireNonNull(response.getBody()).getItems();
    }

    public ProductDetailDto fetchProductById(UUID productId) {
        try {
            ResponseEntity<ProductDetailDto> response = restTemplate.getForEntity(
                    productApiRoot() + "/" + productId, ProductDetailDto.class);
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
                    productApiRoot() + "/batch?ids=" + ids,
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
