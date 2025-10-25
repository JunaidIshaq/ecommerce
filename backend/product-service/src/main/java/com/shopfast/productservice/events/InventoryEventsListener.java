package com.shopfast.productservice.events;

import com.shopfast.productservice.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class InventoryEventsListener {

    private final ProductService productService;

    private final ProcessedEventStore processedEventStore;

    public InventoryEventsListener(ProductService productService, ProcessedEventStore processedEventStore) {
        this.productService = productService;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = "inventory.events", groupId = "product-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryEvent(InventoryEvent event) throws IOException {
        // idempotency: skip if processed
        if(processedEventStore.isProcessed(event.getEventId())) {
            log.info("Skipping already processed event {}", event.getEventId());
            return;
        }

        // idempotency: attempt to mark; if false => already processed
        boolean marked = processedEventStore.markProcessed(event.getEventId());
        if (!marked) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }
        
        try {
            Map<String, Object> p = new HashMap<>();
            String productId = p.get("productId").toString();
            int available = (int) p.get("availableQuantity");
            // update product availability (pseudo-code)
            productService.updateStockAndAvailability(productId, available);
            log.info("Processed inventory event {} for product {}", event.getEventId(), productId);

        } catch (Exception ex) {
            log.error("Failed to process inventory event : {} : {}", event.getEventId(), ex.getMessage(), ex);
            // throw exception to trigger retry / DLQ handling depending on your Kafka listener configuration
            throw ex;
        }
    }
}
