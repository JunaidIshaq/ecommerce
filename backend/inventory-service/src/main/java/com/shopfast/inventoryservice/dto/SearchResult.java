package com.shopfast.inventoryservice.dto;

import com.shopfast.inventoryservice.model.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<InventoryItem> products;

    private long totalHits;

}

