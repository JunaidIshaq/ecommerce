package com.shopfast.adminservice.client;

import com.shopfast.common.events.CartItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name="cart-service", url = "${cart.service.url}")
public interface CartClient {

    @GetMapping("/api/v1/cart/internal")
    public List<CartItemDto> getCartInternal(@RequestHeader("X-User-Id") String userId);

    @DeleteMapping("/api/v1/cart/internal")
    public List<CartItemDto> clearCartInternal(@RequestHeader("X-User-Id") String userId);

}
