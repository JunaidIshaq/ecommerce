package com.shopfast.orderservice.dto;

import com.shopfast.orderservice.model.Order;
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

