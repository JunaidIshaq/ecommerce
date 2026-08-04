package com.shopfast.inventoryservice.service;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.common.dto.ProductDto;
import com.shopfast.common.events.InventoryEvent;
import com.shopfast.inventoryservice.client.ProductClient;
import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
import com.shopfast.inventoryservice.dto.InventoryRequestDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
import com.shopfast.inventoryservice.dto.InventoryWithProductDto;
import com.shopfast.inventoryservice.events.KafkaInventoryProducer;
import com.shopfast.inventoryservice.exception.InsufficientStockException;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.repository.InventoryRepository;
import com.shopfast.inventoryservice.util.InventoryMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final KafkaInventoryProducer kafkaInventoryProducer;

    private final MeterRegistry meterRegistry;

    private final ProductClient productClient;

    // CRUD
    @Transactional
    @CacheEvict(value = {"inventory", "inventoryWithProduct"}, allEntries = true)
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
    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponseDto> getAllInventoryItems(int pageNumber, int pageSize) {
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

    @Cacheable(
            value = "inventoryWithProduct",
            key = "'pageNumber_' + #pageNumber + '_pageSize_' + #pageSize"
    )
    @Transactional(readOnly = true)
    public PagedResponse<InventoryWithProductDto> getAllInventoryItemsWithProduct(int pageNumber, int pageSize) {
        log.info("Fetching all inventory records with product information");
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryItem> inventoryPage = inventoryRepository.findAll(pageable);

        // Resolve every product in ONE remote call instead of one call per row (N+1).
        List<UUID> productIds = inventoryPage.stream().map(InventoryItem::getProductId).toList();
        Map<UUID, ProductDto> productsById = fetchProducts(productIds);

        List<InventoryWithProductDto> inventoryWithProductDtos = inventoryPage.stream()
                .map(item -> InventoryMapper.getInventoryWithProductDto(item, productsById.get(item.getProductId())))
                .toList();

        return new PagedResponse<>(
                inventoryWithProductDtos,
                inventoryPage.getTotalElements(),
                inventoryPage.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    /**
     * Cached read for query paths only.
     *
     * <p><strong>Never call this from a write transaction.</strong> It can return a
     * detached, stale instance; mutating and saving that instance silently reverts
     * concurrent updates. Write paths use {@link #loadForWrite(UUID)} or the atomic
     * repository updates.</p>
     */
    @Cacheable(value = "inventoryByProduct", key = "#productId")
    @Transactional(readOnly = true)
    public InventoryItem getByProductId(UUID productId) {
        return loadForWrite(productId);
    }

    /** Uncached, always-fresh read bound to the current persistence context. */
    private InventoryItem loadForWrite(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product " + productId));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "inventoryByProduct", key = "#productId"),
            // The paged views embed stock numbers, so any quantity change makes every
            // cached page stale. Evicting only the per-product key left the list
            // endpoints serving wrong stock for the whole TTL window.
            @CacheEvict(value = "inventory", allEntries = true),
            @CacheEvict(value = "inventoryWithProduct", allEntries = true)
    })
    public InventoryItem adjustQuantity(UUID productId, AdjustQuantityDto dto) {
        int delta = dto.getQuantityChange();
        if (inventoryRepository.tryAdjust(productId, delta) != 1) {
            // Either the row is missing or the adjustment would drive stock negative.
            loadForWrite(productId); // throws if the product has no inventory row
            throw new InsufficientStockException(productId, Math.abs(delta), "adjustment would reduce below 0");
        }
        InventoryItem item = loadForWrite(productId);
        // 🔁 Call Kafka producer to sync with Product Service
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, delta);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }


    // --- Reserve / Release / Confirm ---
    // All three use a single conditional UPDATE so the availability check and the
    // write are one atomic database operation. A read-check-write here would oversell.

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "inventoryByProduct", key = "#productId"),
            // The paged views embed stock numbers, so any quantity change makes every
            // cached page stale. Evicting only the per-product key left the list
            // endpoints serving wrong stock for the whole TTL window.
            @CacheEvict(value = "inventory", allEntries = true),
            @CacheEvict(value = "inventoryWithProduct", allEntries = true)
    })
    public InventoryItem reserveStock(UUID productId, int quantity) {
        requirePositive(quantity);
        if (inventoryRepository.tryReserve(productId, quantity) != 1) {
            loadForWrite(productId);
            meterRegistry.counter("inventory.reserve.rejected", "productId", productId.toString()).increment();
            throw new InsufficientStockException(productId, quantity, "not enough available stock");
        }
        InventoryItem item = loadForWrite(productId);
        log.info("Reserved {} units of {}", quantity, productId);
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, -quantity);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "inventoryByProduct", key = "#productId"),
            // The paged views embed stock numbers, so any quantity change makes every
            // cached page stale. Evicting only the per-product key left the list
            // endpoints serving wrong stock for the whole TTL window.
            @CacheEvict(value = "inventory", allEntries = true),
            @CacheEvict(value = "inventoryWithProduct", allEntries = true)
    })
    public InventoryItem releaseStock(UUID productId, int quantity) {
        requirePositive(quantity);
        if (inventoryRepository.tryRelease(productId, quantity) != 1) {
            loadForWrite(productId);
            throw new InsufficientStockException(productId, quantity, "not enough reserved stock to release");
        }
        InventoryItem item = loadForWrite(productId);
        log.info("Released {} units of {}", quantity, productId);
        publishStockUpdateEvent("INVENTORY_ADJUSTED", item, quantity);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "inventoryByProduct", key = "#productId"),
            // The paged views embed stock numbers, so any quantity change makes every
            // cached page stale. Evicting only the per-product key left the list
            // endpoints serving wrong stock for the whole TTL window.
            @CacheEvict(value = "inventory", allEntries = true),
            @CacheEvict(value = "inventoryWithProduct", allEntries = true)
    })
    public InventoryItem confirmReservation(UUID productId, int qty) {
        requirePositive(qty);
        if (inventoryRepository.tryConfirm(productId, qty) != 1) {
            loadForWrite(productId);
            throw new InsufficientStockException(productId, qty, "not enough reserved stock to confirm");
        }
        InventoryItem item = loadForWrite(productId);
        log.info("Confirmed {} units sold for {}", qty, productId);
        publishInventoryMetrics(item.getProductId(), item);
        return item;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0, got " + quantity);
        }
    }

    /** One remote call for the whole page; never throws — missing products map to null. */
    private Map<UUID, ProductDto> fetchProducts(List<UUID> productIds) {
        try {
            return productClient.getProductsByIds(productIds);
        } catch (Exception e) {
            log.error("Unable to enrich inventory with product data: {}", e.getMessage());
            return Map.of();
        }
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
