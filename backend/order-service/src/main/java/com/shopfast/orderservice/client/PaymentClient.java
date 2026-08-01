package com.shopfast.orderservice.client;

import com.shopfast.orderservice.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentClient {

    @PostMapping("/api/v1/payment")
    PaymentResponseDto processPayment(@RequestHeader("X-User-Id") String userId,
                                      @RequestBody PaymentRequest paymentRequest);
}
