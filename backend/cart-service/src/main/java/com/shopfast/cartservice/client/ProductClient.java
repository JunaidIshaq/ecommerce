package com.shopfast.cartservice.client;

import com.shopfast.cartservice.dto.ProductInternalResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "product-service")
public interface ProductClient {

   @GetMapping("/api/v1/product/{id}/internal")
   ProductInternalResponseDto getProduct(@PathVariable("id") String id);

}
