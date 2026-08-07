package com.shopfast.cartservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.CartItemRequestDto;
import com.shopfast.cartservice.service.CartService;
import com.shopfast.cartservice.web.CartIdentity;
import com.shopfast.cartservice.web.CartIdentityResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * The cart, for signed-in and anonymous shoppers alike.
 *
 * <p>There is one set of URLs. Whether the request is operating on a user cart or
 * a guest cart is decided by {@link CartIdentityResolver} from the credentials the
 * request actually carries - a verified token if there is one, otherwise the
 * {@code X-Anon-Id} header. The client no longer chooses, because when it did, a
 * stale token in browser storage sent anonymous shoppers to the authenticated URLs
 * and every add-to-cart failed with 401.
 */
@Tag(name = "Cart", description = "Cart APIs")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CartIdentityResolver identityResolver;

    public CartController(CartService cartService, CartIdentityResolver identityResolver) {
        this.cartService = cartService;
        this.identityResolver = identityResolver;
    }

    @Operation(summary = "Add Cart Item")
    @PostMapping("/items")
    public ResponseEntity<Map<String, String>> addItem(@Valid @RequestBody CartItemRequestDto cartItemRequestDto,
                                                       Authentication authentication,
                                                       HttpServletRequest request) throws JsonProcessingException {
        CartIdentity identity = identityResolver.resolve(authentication, request);
        if (identity.guest()) {
            cartService.addGuest(identity.id(), cartItemRequestDto.getProductId(), cartItemRequestDto.getQuantity());
        } else {
            cartService.addUser(identity.id(), cartItemRequestDto.getProductId(), cartItemRequestDto.getQuantity());
        }
        return ok("Cart item added successfully !");
    }

    @Operation(summary = "Update Cart Item Quantity")
    @PutMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> updateItem(@PathVariable("productId") String productId,
                                                          @RequestParam("quantity") Integer quantity,
                                                          Authentication authentication,
                                                          HttpServletRequest request) throws JsonProcessingException {
        CartIdentity identity = identityResolver.resolve(authentication, request);
        if (identity.guest()) {
            cartService.updateGuest(identity.id(), productId, quantity);
        } else {
            cartService.updateUser(identity.id(), productId, quantity);
        }
        return ok("Cart item updated successfully !");
    }

    @Operation(summary = "Get Cart Items")
    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItems(Authentication authentication,
                                                          HttpServletRequest request) {
        CartIdentity identity = identityResolver.resolve(authentication, request);
        return ResponseEntity.ok(identity.guest()
                ? cartService.getGuestCart(identity.id())
                : cartService.getUserCart(identity.id()));
    }

    @Operation(summary = "Delete Product from Cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Map<String, String>> removeItem(@PathVariable("productId") String productId,
                                                          Authentication authentication,
                                                          HttpServletRequest request) throws JsonProcessingException {
        CartIdentity identity = identityResolver.resolve(authentication, request);
        if (identity.guest()) {
            cartService.removeGuestItem(identity.id(), productId);
        } else {
            cartService.removeUserItem(identity.id(), productId);
        }
        return ok("Removed successfully !");
    }

    @Operation(summary = "Clear Cart")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(Authentication authentication,
                                                         HttpServletRequest request) {
        CartIdentity identity = identityResolver.resolve(authentication, request);
        if (identity.guest()) {
            cartService.clearGuestCart(identity.id());
        } else {
            cartService.clearUserCart(identity.id());
        }
        return ok("Cart cleared successfully !");
    }

    private ResponseEntity<Map<String, String>> ok(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "success");
        body.put("message", message);
        return ResponseEntity.ok(body);
    }
}
