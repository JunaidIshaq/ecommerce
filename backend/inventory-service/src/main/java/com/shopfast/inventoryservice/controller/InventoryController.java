package com.shopfast.inventoryservice.controller;

import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
import com.shopfast.inventoryservice.dto.InventoryRequestDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
import com.shopfast.inventoryservice.dto.PagedResponse;
import com.shopfast.inventoryservice.service.InventoryService;
import com.shopfast.inventoryservice.util.InventoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Inventory", description = "Inventory management APIs")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Create inventory record (Admin)")
    @PostMapping
    public ResponseEntity<InventoryResponseDto> create(@Valid @RequestBody InventoryRequestDto dto) {
        return ResponseEntity.ok(inventoryService.createInventoryItem(dto));
    }


    @Operation(summary = "Get all inventory records")
    @GetMapping
    public ResponseEntity<PagedResponse<InventoryResponseDto>> getAllInventoryItems(
            @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ResponseEntity.ok(inventoryService.getAllInventoryItems(pageNumber, pageSize));
    }

    @Operation(summary = "Get inventory by productId")
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDto> getByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(InventoryMapper.getInventoryResponseDto(inventoryService.getByProductId(productId)));
    }

    @Operation(summary = "Adjust stock manually (Admin)")
    @PatchMapping("/{productId}/adjust")
    public ResponseEntity<InventoryResponseDto> adjust(@PathVariable UUID productId,
                                                       @Valid @RequestBody AdjustQuantityDto dto) {
        return ResponseEntity.ok(InventoryMapper.getInventoryResponseDto(inventoryService.adjustQuantity(productId, dto)));
    }

    @Operation(summary = "Reserve stock (User/Order Service)")
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponseDto> reserve(@PathVariable UUID productId,
                                                        @RequestParam("quantity") int qty) {
        return ResponseEntity.ok(InventoryMapper.getInventoryResponseDto(inventoryService.reserveStock(productId, qty)));
    }


    @Operation(summary = "Release reserved stock (Order Canceled)")
    @PostMapping("/{productId}/release")
    public ResponseEntity<InventoryResponseDto> release(@PathVariable UUID productId,
                                                        @RequestParam("quantity") int qty) {
        return ResponseEntity.ok(InventoryMapper.getInventoryResponseDto(inventoryService.releaseStock(productId, qty)));
    }

    @Operation(summary = "Confirm reserved stock (Order Completed)")
    @PostMapping("/{productId}/confirm")
    public ResponseEntity<InventoryResponseDto> confirm(@PathVariable UUID productId,
                                                        @RequestParam("quantity") int qty) {
        return ResponseEntity.ok(InventoryMapper.getInventoryResponseDto(inventoryService.confirmReservation(productId, qty)));
    }

}
