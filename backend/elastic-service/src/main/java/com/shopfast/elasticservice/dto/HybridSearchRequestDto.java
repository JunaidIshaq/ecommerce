package com.shopfast.elasticservice.dto;

import lombok.Data;

@Data
public class HybridSearchRequestDto {

    private String query;

    // filters
    private String category;
    private String brand;
    private Double minPrice;
    private Double maxPrice;

    // pagination
    private int pageNumber = 0;
    private int pageSize = 10;

    // weights
    private double bm25Weight = 0.4;
    private double vectorWeight = 0.6;

}
