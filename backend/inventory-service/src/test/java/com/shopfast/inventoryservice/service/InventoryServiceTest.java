package com.shopfast.inventoryservice.service;

import com.shopfast.common.dto.ProductDto;
import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
import com.shopfast.inventoryservice.events.KafkaInventoryProducer;
import com.shopfast.inventoryservice.exception.InsufficientStockException;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.repository.InventoryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private KafkaInventoryProducer kafkaInventoryProducer;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private com.shopfast.inventoryservice.client.ProductClient productClient;

    @InjectMocks
    private InventoryService inventoryService;

    private io.micrometer.core.instrument.Counter mockCounter;
    @BeforeEach
    void initMocks() {
        mockCounter = org.mockito.Mockito.mock(io.micrometer.core.instrument.Counter.class);
        org.mockito.Mockito.lenient().doReturn(mockCounter).when(meterRegistry).counter(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(java.lang.String[].class));
    }


    private InventoryItem item(UUID productId, int available, int reserved, int sold) {
        return InventoryItem.builder()
                .productId(productId)
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .soldQuantity(sold)
                .build();
    }

    @Test
    void createInventoryItemPersistsWithZeroReserved() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(inv -> {
            InventoryItem it = inv.getArgument(0);
            it.setId(java.util.UUID.randomUUID());
            if (it.getCreatedAt() == null) it.setCreatedAt(java.time.Instant.now());
            if (it.getUpdatedAt() == null) it.setUpdatedAt(java.time.Instant.now());
            return it;
        });

        com.shopfast.inventoryservice.dto.InventoryRequestDto req =
                new com.shopfast.inventoryservice.dto.InventoryRequestDto();
        req.setProductId(productId);
        req.setAvailableQuantity(10);
        var dto = inventoryService.createInventoryItem(req);

        assertThat(dto.getAvailableQuantity()).isEqualTo(10);
        assertThat(dto.getReservedQuantity()).isZero();
    }

    @Test
    void reserveStockSucceedsWhenAvailableEnough() {
        UUID productId = UUID.randomUUID();
        InventoryItem stored = item(productId, 5, 0, 0);
        when(inventoryRepository.tryReserve(productId, 3)).thenReturn(1);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(stored));

        InventoryItem result = inventoryService.reserveStock(productId, 3);

        assertThat(result.getAvailableQuantity()).isEqualTo(5);
        verify(kafkaInventoryProducer).publishInventoryEvent(any());
    }

    @Test
    void reserveStockThrowsWhenInsufficient() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.tryReserve(productId, 99)).thenReturn(0);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item(productId, 2, 0, 0)));

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 99))
                .isInstanceOf(InsufficientStockException.class);
        verify(kafkaInventoryProducer, never()).publishInventoryEvent(any());
    }

    @Test
    void reserveStockRejectsNonPositiveQuantity() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.tryReserve(eq(productId), anyInt())).thenReturn(0);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item(productId, 5, 0, 0)));

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releaseStockSucceeds() {
        UUID productId = UUID.randomUUID();
        InventoryItem stored = item(productId, 5, 2, 0);
        when(inventoryRepository.tryRelease(productId, 2)).thenReturn(1);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(stored));

        InventoryItem result = inventoryService.releaseStock(productId, 2);

        assertThat(result.getReservedQuantity()).isEqualTo(2);
        verify(kafkaInventoryProducer).publishInventoryEvent(any());
    }

    @Test
    void confirmReservationSucceeds() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.tryConfirm(productId, 2)).thenReturn(1);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item(productId, 5, 2, 0)));

        inventoryService.confirmReservation(productId, 2);

        // confirmReservation records only metrics, it does not publish a stock event
        verify(kafkaInventoryProducer, org.mockito.Mockito.never()).publishInventoryEvent(any());
    }

    @Test
    void adjustQuantitySucceedsWhenStockAllows() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.tryAdjust(productId, -2)).thenReturn(1);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item(productId, 10, 0, 0)));

        com.shopfast.inventoryservice.dto.AdjustQuantityDto adj =
                new com.shopfast.inventoryservice.dto.AdjustQuantityDto();
        adj.setQuantityChange(-2);
        inventoryService.adjustQuantity(productId, adj);

        verify(kafkaInventoryProducer).publishInventoryEvent(any());
    }

    @Test
    void adjustQuantityThrowsWhenWouldGoNegative() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.tryAdjust(productId, -20)).thenReturn(0);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(item(productId, 5, 0, 0)));

        com.shopfast.inventoryservice.dto.AdjustQuantityDto adj2 =
                new com.shopfast.inventoryservice.dto.AdjustQuantityDto();
        adj2.setQuantityChange(-20);
        assertThatThrownBy(() -> inventoryService.adjustQuantity(productId, adj2))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void getAllInventoryItemsWithProductEnrichesViaSingleCall() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(productClient.getProductsByIds(any())).thenReturn(Map.of(productId, new ProductDto()));

        inventoryService.getAllInventoryItemsWithProduct(1, 10);

        verify(productClient).getProductsByIds(any());
    }

    @Test
    void getAllInventoryItemsWithProductSwallowsClientFailure() {
        when(inventoryRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(productClient.getProductsByIds(any())).thenThrow(new RuntimeException("boom"));

        // Should not throw
        inventoryService.getAllInventoryItemsWithProduct(1, 10);
    }
}
