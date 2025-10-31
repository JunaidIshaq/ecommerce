package com.shopfast.paymentservice.controller;

import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.dto.PaymentResponseDto;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payments", description = "Payment processing APIs")
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;


    public PaymentController(PaymentService paymentService1) {
        this.paymentService = paymentService1;
    }

    @Operation(summary = "Initiate payment for an order")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto req) {
        Payment saved = paymentService.processPayment(req);
        PaymentResponseDto res = PaymentResponseDto.builder()
                .id(saved.getId())
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .status(saved.getStatus())
                .transactionId(saved.getTransactionId())
                .createdAt(saved.getCreatedAt().toString())
                .updatedAt(saved.getUpdatedAt().toString())
                .build();
        return ResponseEntity.ok(res);
    }


    @Operation(summary = "Simple webhook receiver (gateway callbacks)")
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body) {
        // For a real gateway, verify signatures and update payment records accordingly.
        // For now just log.
        System.out.println("Received webhook: " + body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable UUID paymentId) {
        Payment payment = paymentService.getById(paymentId);
        PaymentResponseDto res = PaymentResponseDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt().toString())
                .updatedAt(payment.getUpdatedAt().toString())
                .build();
        return ResponseEntity.ok(res);
    }

}
