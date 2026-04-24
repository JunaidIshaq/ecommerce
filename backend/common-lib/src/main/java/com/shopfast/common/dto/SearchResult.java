package com.shopfast.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> products;

    private long totalHits;

}
