package com.shopfast.elasticservice.service;

import com.shopfast.elasticservice.document.ProductDocument;

import java.util.List;

public interface ProductSearchService {

    List<ProductDocument> semanticSearch(String query, int k);

}
