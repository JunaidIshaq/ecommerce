package com.shopfast.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse <T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> items;   // List of items
    private long totalItems; // Total count across all pages
    private int totalPages;  // Total number of pages
    private int page;        // Current page (0-based)
    private int size;        // Page size

}
