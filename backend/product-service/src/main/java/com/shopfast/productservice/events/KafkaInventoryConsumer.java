package com.shopfast.productservice.events;

import com.shopfast.common.events.InventoryEvent;
import com.shopfast.productservice.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for inventory events
 */
@Slf4j
@Component
public class KafkaInventoryConsumer {

    private final ProductService productService;

    private final ProcessedEventStore processedEventStore;

    public KafkaInventoryConsumer(ProductService productService, ProcessedEventStore processedEventStore) {
        this.productService = productService;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = "inventory.events", groupId = "product-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryEvent(InventoryEvent event) {
        // Idempotency: skip if already processed
        if (processedEventStore.isProcessed(event.getEventId())) {
            log.info("Skipping already processed event {}", event.getEventId());
            return;
        }

        try {
            Map<String, Object> payload = event.getPayload();
            String productId = payload.get("productId").toString();
            int available = ((Number) payload.get("availableQuantity")).intValue();

            // Update product availability
            productService.updateStockAndAvailability(productId, available);

            // Mark only AFTER the update succeeded. Marking first meant a failed handler
            // was replayed, found itself flagged as "processed", and silently dropped the
            // event - leaving product stock permanently out of sync with inventory.
            processedEventStore.markProcessed(event.getEventId());
            log.info("Processed inventory event {} for product {}", event.getEventId(), productId);

        } catch (Exception ex) {
            log.error("Failed to process inventory event {}: {}", event.getEventId(), ex.getMessage(), ex);
            // Throw exception to trigger retry / DLQ handling depending on Kafka listener configuration
            throw ex;
        }
    }
}
