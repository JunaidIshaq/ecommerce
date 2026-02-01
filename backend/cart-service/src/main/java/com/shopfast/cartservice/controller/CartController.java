package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
import com.shopfast.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<Map<String, String>> addItem(@Valid @RequestBody CartItemRequestDto cartItemRequestDto, Authentication authentication) throws JsonProcessingException {
        String userId = authentication.getName();
        cartService.addOrUpdateUser(userId, cartItemRequestDto.getProductId(), cartItemRequestDto.getQuantity());
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item added successfully !");
        return ResponseEntity.ok(map);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> updateItem(@PathVariable String productId, @RequestParam("quantity") Integer quantity, Authentication authentication) throws JsonProcessingException {
        String userId = authentication.getName();
        cartService.addOrUpdateUser(userId, productId, quantity);
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart item updated successfully !");
        return ResponseEntity.ok(map);
    }


    @Operation(summary = "Get Cart Items")
    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItems(Authentication authentication) {
        String userId = authentication.getName();
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart items retrieved successfully !");
        return ResponseEntity.ok(cartService.getUserCart(userId));
    }

    @Operation(summary = "Delete Product form Cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> removeItem(@PathVariable String productId, Authentication authentication) throws JsonProcessingException {
        cartService.removeUserItem(authentication.getName(), productId);
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Removed successfully !");
        return ResponseEntity.ok(map);
    }

    @Operation(summary = "Clear Cart")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(Authentication authentication) {
        cartService.clearUserCart(authentication.getName());
        Map<String, String> map = new HashMap<>();
        map.put("status", "success");
        map.put("message", "Cart cleared successfully !");
        return ResponseEntity.ok(map);
    }


}
