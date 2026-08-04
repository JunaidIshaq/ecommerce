package com.shopfast.inventoryservice.exception;

import java.util.UUID;

/**
 * Thrown when an atomic stock movement is refused by the database because the
 * required quantity is not available/reserved.
 *
 * <p>Distinct from {@code IllegalArgumentException} so callers (and Kafka error
 * handlers) can treat it as a non-retryable business outcome rather than a bug.</p>
 */
public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final int requestedQuantity;

    public InsufficientStockException(UUID productId, int requestedQuantity, String detail) {
        super("Insufficient stock for product " + productId + " (requested " + requestedQuantity + "): " + detail);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
