package com.shopfast.productservice.dto;

import com.shopfast.productservice.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<Product> products;

    private long totalHits;

}

