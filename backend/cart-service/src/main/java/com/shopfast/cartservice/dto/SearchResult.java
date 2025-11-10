package com.shopfast.cartservice.dto;

import com.shopfast.cartservice.model.Order;
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

