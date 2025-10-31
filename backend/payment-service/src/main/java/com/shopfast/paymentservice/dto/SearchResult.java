package com.shopfast.paymentservice.dto;

import com.shopfast.paymentservice.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<Payment> products;

    private long totalHits;

}

