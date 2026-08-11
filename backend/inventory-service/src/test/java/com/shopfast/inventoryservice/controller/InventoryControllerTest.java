package com.shopfast.inventoryservice.controller;

import com.shopfast.common.dto.PagedResponse;
import com.shopfast.inventoryservice.dto.AdjustQuantityDto;
import com.shopfast.inventoryservice.dto.InventoryRequestDto;
import com.shopfast.inventoryservice.dto.InventoryResponseDto;
import com.shopfast.inventoryservice.dto.InventoryWithProductDto;
import com.shopfast.inventoryservice.model.InventoryItem;
import com.shopfast.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InventoryController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        },
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = Configuration.class))
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private InventoryService inventoryService;

    private InventoryItem sampleItem(UUID productId) {
        return InventoryItem.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .availableQuantity(100)
                .reservedQuantity(0)
                .soldQuantity(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void create_returnsOkAndBody() throws Exception {
        UUID productId = UUID.randomUUID();
        InventoryRequestDto req = new InventoryRequestDto();
        req.setProductId(productId);
        req.setAvailableQuantity(50);

        InventoryResponseDto resp = new InventoryResponseDto();
        resp.setId(UUID.randomUUID().toString());
        resp.setProductId(productId.toString());
        resp.setAvailableQuantity(50);
        when(inventoryService.createInventoryItem(org.mockito.ArgumentMatchers.any(InventoryRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()))
                .andExpect(jsonPath("$.available_quantity").value(50));
    }

    @Test
    void getAllInventoryItems_returnsOk() throws Exception {
        PagedResponse<InventoryResponseDto> page = new PagedResponse<>(
                List.of(new InventoryResponseDto()), 1, 1, 1, 10);
        when(inventoryService.getAllInventoryItems(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/inventory/pageNumber/1/pageSize/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getAllAdminInventoryItems_returnsOk() throws Exception {
        PagedResponse<InventoryWithProductDto> page = new PagedResponse<>(
                List.of(new InventoryWithProductDto()), 1, 1, 1, 10);
        when(inventoryService.getAllInventoryItemsWithProduct(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/inventory/internal/admin/inventory/pageNumber/1/pageSize/10")
                        .header("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getByProductId_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.getByProductId(productId)).thenReturn(sampleItem(productId));

        mockMvc.perform(get("/api/v1/inventory/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()));
    }

    @Test
    void adjust_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        AdjustQuantityDto dto = new AdjustQuantityDto(5);
        when(inventoryService.adjustQuantity(productId, dto)).thenReturn(sampleItem(productId));

        mockMvc.perform(patch("/api/v1/inventory/" + productId + "/adjust")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()));
    }

    @Test
    void reserve_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.reserveStock(productId, 2)).thenReturn(sampleItem(productId));

        mockMvc.perform(post("/api/v1/inventory/" + productId + "/reserve")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()));
    }

    @Test
    void release_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.releaseStock(productId, 2)).thenReturn(sampleItem(productId));

        mockMvc.perform(post("/api/v1/inventory/" + productId + "/release")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()));
    }

    @Test
    void confirm_returnsOk() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.confirmReservation(productId, 2)).thenReturn(sampleItem(productId));

        mockMvc.perform(post("/api/v1/inventory/" + productId + "/confirm")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(productId.toString()));
    }
}
