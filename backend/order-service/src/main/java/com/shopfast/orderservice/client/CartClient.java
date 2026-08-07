package com.shopfast.orderservice.client;

import com.shopfast.common.events.CartItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * A basket belongs either to a signed-in shopper ({@code X-User-Id}) or to a
 * guest ({@code X-Anon-Id}); both headers are optional here so the caller can
 * pick the right one at runtime. Feign omits a header whose value is null.
 */
@FeignClient(name="cart-service", url = "${cart.service.url}")
public interface CartClient {

    @GetMapping("/api/v1/cart/internal")
    List<CartItemDto> getCartInternal(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                      @RequestHeader(value = "X-Anon-Id", required = false) String anonId);

    @DeleteMapping("/api/v1/cart/internal")
    List<CartItemDto> clearCartInternal(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                        @RequestHeader(value = "X-Anon-Id", required = false) String anonId);

    default List<CartItemDto> getCartInternal(String userId) {
        return getCartInternal(userId, null);
    }

    default List<CartItemDto> clearCartInternal(String userId) {
        return clearCartInternal(userId, null);
    }
}
