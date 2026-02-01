package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
import com.shopfast.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart/guest")
public class CartGuestController {

    private final CartService cartService;

    public CartGuestController(CartService cartService) {
        this.cartService = cartService;
    }

    // anonId is a client-generated UUID (e.g., stored in localStorage)
    @PostMapping("/items")
    public ResponseEntity<Map<String, String>> addItem(@RequestParam("anonId") String anonId, @Valid @RequestBody CartItemRequestDto requestDto) throws JsonProcessingException {
        cartService.addGuest(anonId, requestDto.getProductId(), requestDto.getQuantity());
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item added successfully !");
        return ResponseEntity.ok(map);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> updateItem(@PathVariable String productId, @RequestParam("anonId") String anonId, @RequestParam("quantity") Integer quantity) throws JsonProcessingException {
        cartService.updateGuest(anonId, productId, quantity);
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item updated successfully !");
        return ResponseEntity.ok(map);
    }

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItem(@RequestParam String anonId) {
        return ResponseEntity.ok(cartService.getGuestCart(anonId));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> removeItem(@RequestParam String anonId, @PathVariable String productId) {
        cartService.removeGuestItem(anonId, productId);
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item removed successfully !");
        return ResponseEntity.ok(map);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clear(@RequestParam String anonId) {
        cartService.clearGuestCart(anonId);
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item cleared successfully !");
        return ResponseEntity.ok(map);
    }



}
