package com.shopfast.orderservice.controller;

import com.shopfast.orderservice.dto.CheckoutRequestDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.service.CheckoutService;
import com.shopfast.orderservice.util.OrderMapper;
import com.shopfast.orderservice.web.OrderIdentity;
import com.shopfast.orderservice.web.OrderIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final OrderIdentityResolver identityResolver;

    public CheckoutController(CheckoutService checkoutService, OrderIdentityResolver identityResolver) {
        this.checkoutService = checkoutService;
        this.identityResolver = identityResolver;
    }

    /**
     * The buyer used to come from an {@code X-User-Id} request header, so any caller
     * could place an order in another shopper's name - and then read it back - just
     * by changing that header. It now comes from the token, falling back to the
     * anonymous browser id for guest checkout.
     *
     * <p>For a guest the response carries a one-time {@code accessToken}; it is the
     * only way they can open the order afterwards, since they have no JWT.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(Authentication authentication,
                                                     HttpServletRequest httpRequest,
                                                     @RequestBody(required = false) CheckoutRequestDto checkoutRequestDto) {
        OrderIdentity buyer = identityResolver.resolve(authentication, httpRequest);
        Order order = checkoutService.checkout(buyer, checkoutRequestDto);

        OrderResponseDto response = OrderMapper.getOrderResponseDto(order);
        if (buyer.guest()) {
            response.setAccessToken(order.getAccessToken());
        }
        return ResponseEntity.ok(response);
    }
}
