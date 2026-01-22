package com.shopfast.elasticservice.service;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.dto.HybridSearchRequestDto;

import java.util.List;

public interface HybridSearchService {

    List<ProductDocument> search(HybridSearchRequestDto hybridSearchRequestDto);

}
