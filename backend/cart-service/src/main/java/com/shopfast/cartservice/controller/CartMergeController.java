package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.GuestMergeRequestDto;
import com.shopfast.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartMergeController {

    private final CartService cartService;

    public CartMergeController(CartService cartService) {
        this.cartService = cartService;
    }

    // Call right after successful login
    @PostMapping("/merge")
    public ResponseEntity<Void> merge(@Valid @RequestBody GuestMergeRequestDto guestMergeRequestDto, Authentication authentication) throws JsonProcessingException {
        String userId = authentication.getName();
        cartService.mergeGuestIntoUser(guestMergeRequestDto.getAnonId(), userId);
        return ResponseEntity.ok().build();
    }
}
