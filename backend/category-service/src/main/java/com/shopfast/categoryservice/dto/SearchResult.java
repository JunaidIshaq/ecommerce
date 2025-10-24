package com.shopfast.categoryservice.dto;

import com.shopfast.categoryservice.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<Category> categories;

    private long totalHits;

}

