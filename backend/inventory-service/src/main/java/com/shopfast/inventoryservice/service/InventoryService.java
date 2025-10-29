package com.shopfast.inventoryservice.service;

import com.shopfast.common.events.InventoryEvent;
import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
import com.shopfast.inventoryservice.dto.InventoryRequestDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
import com.shopfast.inventoryservice.dto.PagedResponse;
import com.shopfast.inventoryservice.events.KafkaInventoryProducer;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.repository.InventoryRepository;
import com.shopfast.inventoryservice.util.InventoryMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final KafkaInventoryProducer kafkaInventoryProducer;

    private final MeterRegistry meterRegistry;

    public InventoryService(InventoryRepository inventoryRepository, KafkaInventoryProducer kafkaInventoryProducer, MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaInventoryProducer = kafkaInventoryProducer;
        this.meterRegistry = meterRegistry;
    }

    // CRUD
    @CacheEvict(value = "inventory", allEntries = true)
    public InventoryResponseDto createInventoryItem(InventoryRequestDto dto) {
        log.info("Creating inventory for product {}", dto.getProductId());
        InventoryItem inventoryItem = InventoryItem.builder()
                .productId(dto.getProductId())
                .availableQuantity(dto.getAvailableQuantity())
                .reservedQuantity(0)
                .soldQuantity(0)
                .build();
        return InventoryMapper.getInventoryResponseDto(inventoryRepository.save(inventoryItem));
    }

    @Cacheable(
            value = "inventory",
            key = "'pageNumber_' + #pageNumber + '_pageSize_' + #pageSize"
    )
    public PagedResponse<InventoryResponseDto> getAllInventoryItems(
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        log.info("Fetching all inventory records");
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryItem> inventoryPage = inventoryRepository.findAll(pageable);
        List<InventoryResponseDto> inventoryResponseDtos = inventoryPage.stream().map(InventoryMapper::getInventoryResponseDto).toList();
        return new PagedResponse<>(
                inventoryResponseDtos,
                inventoryPage.getTotalElements(),
                inventoryPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    @Cacheable(value = "inventoryByProduct", key = "#productId")
    public InventoryItem getByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product " + productId));
    }

    @Transactional
    @CacheEvict(value = {"inventory", "inventoryByProduct"}, allEntries = true)
    public InventoryItem adjustQuantity(UUID productId, AdjustQuantityDto dto) {
        InventoryItem item = getByProductId(productId);
        int newQty = item.getAvailableQuantity() + dto.getQuantityChange();
        if (newQty < 0)
            throw new IllegalArgumentException("Cannot reduce below 0");
        item.setAvailableQuantity(newQty);
        inventoryRepository.save(item);
        // 🔁 Call Kafka producer to sync with Product Service
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, dto.getQuantityChange());
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }


    // --- Reserve / Release / Confirm ---

    @Transactional
    @CacheEvict(value = {"inventory", "inventoryByProduct"}, allEntries = true)
    public InventoryItem reserveStock(UUID productId, int quantity) {
        InventoryItem item = getByProductId(productId);
        if (item.getAvailableQuantity() < quantity)
            throw new IllegalArgumentException("Insufficient stock");
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        item.setReservedQuantity(item.getReservedQuantity() + quantity);
        log.info("Reserved {} units of {}", quantity, productId);
        inventoryRepository.save(item);
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, quantity);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    @Transactional
    @CacheEvict(value = {"inventory", "inventoryByProduct"}, allEntries = true)
    public InventoryItem releaseStock(UUID productId, int quantity) {
        InventoryItem item = getByProductId(productId);
        if (item.getReservedQuantity() < quantity)
            throw new IllegalArgumentException("Not enough reserved stock to release");
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
        log.info("Released {} units of {}", quantity, productId);
        inventoryRepository.save(item);
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, quantity);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    @Transactional
    @CacheEvict(value = {"inventory", "inventoryByProduct"}, allEntries = true)
    public InventoryItem confirmReservation(UUID productId, int qty) {
        InventoryItem item = getByProductId(productId);
        if (item.getReservedQuantity() < qty)
            throw new IllegalArgumentException("Not enough reserved stock to confirm");
        item.setReservedQuantity(item.getReservedQuantity() - qty);
        item.setSoldQuantity(item.getSoldQuantity() + qty);
        log.info("Confirmed {} units sold for {}", qty, productId);
        item = inventoryRepository.save(item);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    private void publishStockUpdateEvent(String type, InventoryItem item, int quantityChange) {
        InventoryEvent event = new InventoryEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(type);
        event.setEventVersion(1);
        event.setOccurredAt(Instant.now());
        event.setPayload(Map.of(
                "productId", item.getProductId().toString(),
                "availableQuantity", item.getAvailableQuantity(),
                "reservedQuantity", item.getReservedQuantity(),
                "soldQuantity", item.getSoldQuantity(),
                "change", quantityChange,
                "source", "inventory-service"
        ));

        kafkaInventoryProducer.publishInventoryEvent(event);
    }

    // Call this after changes (create/adjust/reserve/release/confirm)
    private void publishInventoryMetrics(UUID productId, InventoryItem item) {
        String name = "inventory.available";
        meterRegistry.gauge(name, Tags.of("productId", productId.toString()), item.getAvailableQuantity());
        // a counter for reservations
        meterRegistry.counter("inventory.reservations.count", "productId", productId.toString())
                .increment(); // call when reservation happens
    }


}
