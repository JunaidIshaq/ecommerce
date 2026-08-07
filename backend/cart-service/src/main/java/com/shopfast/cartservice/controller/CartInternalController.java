package com.shopfast.cartservice.controller;

import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Service-to-service view of a basket, used by order-service during checkout.
 *
 * <p>Stays behind authentication (it is not in {@code public-paths}) because the
 * caller asserts whose cart it wants. A guest basket is addressed with
 * {@code X-Anon-Id} rather than {@code X-User-Id}: without that, checking out as
 * a guest would read an empty user cart and fail with "Cart is empty".
 */
@RestController
@RequestMapping("/api/v1/cart/internal")
public class CartInternalController {

    private final CartService cartService;

    public CartInternalController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId) {
        return ResponseEntity.ok(hasText(userId)
                ? cartService.getCartItems(userId)
                : cartService.getGuestCart(requireAnonId(anonId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId) {
        if (hasText(userId)) {
            cartService.clearCart(userId);
        } else {
            cartService.clearGuestCart(requireAnonId(anonId));
        }
        return ResponseEntity.ok().build();
    }

    private String requireAnonId(String anonId) {
        if (!hasText(anonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either X-User-Id or X-Anon-Id is required");
        }
        return anonId.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
