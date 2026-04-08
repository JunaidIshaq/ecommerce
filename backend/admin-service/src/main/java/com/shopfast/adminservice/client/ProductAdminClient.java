package com.shopfast.adminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url="${product.service.url}", path = "/api/v1/product")
public interface ProductAdminClient {

    @GetMapping("/internal/admin/product/pageNumber/{pageNumber}/pageSize/{pageSize}")
    Object getAllProducts(@RequestHeader("userId") String id, @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize);

    @PutMapping("/{id}/status")
    void updateProductStatus(@PathVariable("id") Long id,
                           @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
                           @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                           @RequestParam("status") String status);
}
