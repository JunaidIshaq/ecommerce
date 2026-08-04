package com.shopfast.cartservice.client;

import com.shopfast.cartservice.dto.ProductInternalResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Circuit-breaker wrapper around product-service lookups.
 *
 * <p>Every cart mutation validates the product first, so an unprotected client
 * lets a slow product-service pin every cart request thread until its socket
 * timeout.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductGateway {

    private final ProductClient productClient;

    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
    @Retry(name = "product-service")
    public ProductInternalResponseDto getProduct(String productId) {
        return productClient.getProduct(productId);
    }

    @SuppressWarnings("unused") // referenced by name from @CircuitBreaker
    private ProductInternalResponseDto getProductFallback(String productId, Throwable t) {
        // Callers treat null as "product not purchasable" and reject the write.
        // Returning null here is safe precisely because it fails closed: we never
        // add an unvalidated item (and therefore an unvalidated price) to a cart.
        log.warn("product-service unavailable while validating product {}: {}", productId, t.toString());
        return null;
    }
}
