package com.shopfast.inventoryservice.events;

import com.shopfast.common.events.InventoryEvent;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.repository.InventoryRepository;
import com.shopfast.inventoryservice.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class KafkaOrderConsumer {

    private final InventoryService inventoryService;

    private final KafkaInventoryProducer kafkaInventoryProducer;

    private final InventoryRepository inventoryRepository;

    public KafkaOrderConsumer(InventoryService inventoryService, KafkaInventoryProducer kafkaInventoryProducer, InventoryRepository inventoryRepository) {
        this.inventoryService = inventoryService;
        this.kafkaInventoryProducer = kafkaInventoryProducer;
        this.inventoryRepository = inventoryRepository;
    }

    @KafkaListener(topics = "order.commands", groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCommand(OrderCommand command) {
        log.info("📦 Received order command: {}", command);

        String commandType = command.getCommandType().toUpperCase();
        Map<String, Object> payload = command.getPayload();

        UUID productId = UUID.fromString((String) payload.get("productId"));
        int quantity = (Integer) payload.get("quantity");

        try {
            switch (commandType) {
                case "RESERVE" -> {
                    inventoryService.reserveStock(productId, quantity);
                    publishInventoryEvent("INVENTORY_RESERVED", productId, quantity);
                }
                case "RELEASE" -> {
                    inventoryService.releaseStock(productId, quantity);
                    publishInventoryEvent("INVENTORY_RELEASED", productId, -quantity);
                }
                case "CONFIRM" -> {
                    inventoryService.confirmReservation(productId, quantity);
                    publishInventoryEvent("INVENTORY_CONFIRMED", productId, -quantity);
                }
                default -> log.warn("⚠️ Unknown command type: {}", commandType);
            }
        } catch (Exception ex) {
            log.error("❌ Error handling command {}: {}", commandType, ex.getMessage(), ex);
            throw ex;
        }
    }

    private void publishInventoryEvent(String type, UUID productId, int qtyChange) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in inventory: " + productId));

        InventoryEvent event = new InventoryEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(type);
        event.setEventVersion(1);
        event.setOccurredAt(Instant.now());
        event.setPayload(Map.of(
                "productId", productId.toString(),
                "availableQuantity", item.getAvailableQuantity(),
                "reservedQuantity", item.getReservedQuantity(),
                "soldQuantity", item.getSoldQuantity(),
                "change", qtyChange,
                "source", "inventory-service"
        ));

        kafkaInventoryProducer.publishInventoryEvent(event);
    }

}
