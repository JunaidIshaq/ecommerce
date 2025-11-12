package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
import com.shopfast.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart/guest")
public class CartGuestController {

    private final CartService cartService;

    public CartGuestController(CartService cartService) {
        this.cartService = cartService;
    }

    // anonId is a client-generated UUID (e.g., stored in localStorage)
    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@RequestParam String anonId, @Valid @RequestBody CartItemRequestDto requestDto) throws JsonProcessingException {
        cartService.addOrUpdateGuest(anonId, requestDto.getProductId(), requestDto.getQuantity());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItem(@RequestParam String anonId) {
        return ResponseEntity.ok(cartService.getGuestCart(anonId));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@RequestParam String anonId, @PathVariable String productId) {
        cartService.removeGuestItem(anonId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@RequestParam String anonId) {
        cartService.clearGuestCart(anonId);
        return ResponseEntity.ok().build();
    }



}
