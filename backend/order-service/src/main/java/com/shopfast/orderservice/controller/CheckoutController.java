package com.shopfast.orderservice.controller;

import com.shopfast.orderservice.dto.CheckoutRequestDto;
import com.shopfast.orderservice.dto.OrderResponseDto;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(@RequestHeader("X-User-Id") String userId,
                                                     @RequestBody(required = false) CheckoutRequestDto checkoutRequestDto) {
        String coupon = (checkoutRequestDto != null) ? checkoutRequestDto.getCouponCode() : null;
        Order order = checkoutService.checkout(userId, checkoutRequestDto);
        return ResponseEntity.ok(OrderResponseDto.from(order));
    }
}
