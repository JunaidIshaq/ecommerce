package com.shopfast.elasticservice.controller;

import com.shopfast.elasticservice.document.ProductDocument;
import com.shopfast.elasticservice.service.ProductIndexService;
import com.shopfast.elasticservice.service.ProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search/product")
public class ProductSearchController {

    private final ProductIndexService indexService;
    private final ProductSearchService searchService;

    public ProductSearchController(ProductIndexService indexService, ProductSearchService searchService) {
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public ProductDocument index(@RequestBody ProductDocument product) {
        return indexService.index(product);
    }

    @GetMapping("/semantic")
    public List<ProductDocument> semanticSearch(@RequestParam String q,
                                                @RequestParam(defaultValue = "10") int k) {
        return searchService.semanticSearch(q, k);
    }
}
