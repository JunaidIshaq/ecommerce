package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.OrderResponseDto;
import com.shopfast.cartservice.model.Order;
import com.shopfast.cartservice.service.CartService;
import com.shopfast.cartservice.util.OrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cart", description = "Cart APIs")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;


    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "Add Cart Item")
    @PostMapping("/items")
    public ResponseEntity<String> addItem(@RequestParam String productId, @RequestParam Integer quantity, Authentication authentication) throws JsonProcessingException {
        String userId = authentication.getName();
        cartService.addItem(userId, productId, quantity);
        return ResponseEntity.ok("Cart Item Added");
    }

    @Operation(summary = "Get Cart Items")
    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItems(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(cartService.getCartItems(userId));
    }

    @Operation(summary = "Delete Product form Cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable String productId, Authentication authentication) throws JsonProcessingException {
        cartService.removeItem(authentication.getName(), productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear Cart")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@PathVariable String productId, Authentication authentication) throws JsonProcessingException {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok().build();
    }

}
