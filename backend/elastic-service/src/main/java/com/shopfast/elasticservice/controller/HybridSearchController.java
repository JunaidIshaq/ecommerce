package com.shopfast.elasticservice.controller;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.dto.HybridSearchRequestDto;
import com.shopfast.elasticservice.service.HybridSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search/product")
public class HybridSearchController {

    private final HybridSearchService hybridSearchService;

    public HybridSearchController(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/hybrid")
    public List<ProductDocument> hybridSearch(@RequestBody HybridSearchRequestDto requestDto) {
        return hybridSearchService.search(requestDto);
    }
}
