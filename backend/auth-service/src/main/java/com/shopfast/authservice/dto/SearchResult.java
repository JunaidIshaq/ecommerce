package com.shopfast.authservice.dto;

import com.shopfast.authservice.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<Order> products;

    private long totalHits;

}

