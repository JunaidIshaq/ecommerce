package com.shopfast.inventoryservice.events;

import com.shopfast.common.events.ProductEvent;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class KafkaProductConsumer {

    private final InventoryRepository inventoryRepository;

    public KafkaProductConsumer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @KafkaListener(topics = "product.events", groupId = "inventory-service-group")
    public void onProductEvent(ProductEvent event) {
        if (event == null) {
            log.warn("⚠️ Received null ProductEvent");
            return;
        }

        String eventType = event.getEventType();
        Map<String, Object> payload = event.getPayload();

        if (!"PRODUCT_CREATED".equalsIgnoreCase(eventType)) {
            log.info("Skipping event type: {}", eventType);
            return;
        }

        try {
            String productIdStr = (String) payload.get("productId");
            UUID productId = UUID.fromString(productIdStr);

            Optional<InventoryItem> existing = inventoryRepository.findByProductId(productId);
            if (existing.isPresent()) {
                log.info("Inventory already exists for product {}, skipping.", productId);
                return;
            }

            InventoryItem newItem = InventoryItem.builder()
                    .productId(productId)
                    .availableQuantity(0)
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .build();
            inventoryRepository.save(newItem);

            log.info("✅ Created inventory entry for new product {}", productId);
        } catch (Exception e) {
            log.error("❌ Failed to process ProductEvent: {}", e.getMessage(), e);
        }
    }
}
