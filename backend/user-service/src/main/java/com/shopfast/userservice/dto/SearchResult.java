package com.shopfast.userservice.dto;

import com.shopfast.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<User> products;

    private long totalHits;

}

