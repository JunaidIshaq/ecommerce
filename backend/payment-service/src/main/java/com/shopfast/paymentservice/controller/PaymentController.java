package com.shopfast.paymentservice.controller;

import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.dto.PaymentResponseDto;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Payments", description = "Payment processing APIs")
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final com.shopfast.paymentservice.config.StripeProperties stripeProperties;

    public PaymentController(PaymentService paymentService1, 
                            com.shopfast.paymentservice.config.StripeProperties stripeProperties) {
        this.paymentService = paymentService1;
        this.stripeProperties = stripeProperties;
    }

    @Operation(summary = "Initiate payment for an order")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto req) throws StripeException {
        Payment saved = paymentService.processPayment(req);
        PaymentResponseDto res = PaymentResponseDto.builder()
                .id(saved.getId())
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .status(saved.getStatus())
                .transactionId(saved.getTransactionId())
                .clientSecret(saved.getClientSecret())
                .paymentMethod(saved.getMethod() != null ? saved.getMethod().name() : null)
                .createdAt(saved.getCreatedAt().toString())
                .updatedAt(saved.getUpdatedAt().toString())
                .build();
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Stripe webhook endpoint")
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String body, 
                                          @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Received Stripe webhook");
        
        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(
                body, 
                sigHeader, 
                stripeProperties.getWebhookSecret()
            );

            log.info("Stripe webhook event type: {}", event.getType());

            // Handle the event
            if ("payment_intent.succeeded".equals(event.getType())) {
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) 
                    dataObjectDeserializer.getObject().orElse(null);
                
                if (paymentIntent != null) {
                    String paymentIntentId = paymentIntent.getId();
                    String status = paymentIntent.getStatus().toUpperCase();
                    log.info("PaymentIntent {} status: {}", paymentIntentId, status);
                    
                    // Update payment in database
                    paymentService.updatePaymentFromWebhook(paymentIntentId, status, paymentIntentId);
                    
                    // Publish payment success event
                    if ("succeeded".equals(status)) {
                        // You can also publish a Kafka event here if needed
                        log.info("Payment succeeded for PaymentIntent: {}", paymentIntentId);
                    }
                }
            } else if ("payment_intent.payment_failed".equals(event.getType())) {
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) 
                    dataObjectDeserializer.getObject().orElse(null);
                
                if (paymentIntent != null) {
                    String paymentIntentId = paymentIntent.getId();
                    String status = paymentIntent.getStatus().toUpperCase();
                    log.info("PaymentIntent {} failed: {}", paymentIntentId, status);
                    
                    paymentService.updatePaymentFromWebhook(paymentIntentId, status, null);
                }
            } else if ("payment_intent.canceled".equals(event.getType())) {
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) 
                    dataObjectDeserializer.getObject().orElse(null);
                
                if (paymentIntent != null) {
                    String paymentIntentId = paymentIntent.getId();
                    log.info("PaymentIntent {} canceled", paymentIntentId);
                    paymentService.updatePaymentFromWebhook(paymentIntentId, "CANCELED", null);
                }
            }

            return ResponseEntity.ok().body("{\"status\":\"success\"}");
            
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            return ResponseEntity.status(400).body("{\"error\":\"Invalid signature\"}");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(500).body("{\"error\":\"Webhook processing failed\"}");
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable("paymentId") UUID paymentId) {
        Payment payment = paymentService.getById(paymentId);
        PaymentResponseDto res = PaymentResponseDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .clientSecret(payment.getClientSecret())
                .paymentMethod(payment.getMethod() != null ? payment.getMethod().name() : null)
                .createdAt(payment.getCreatedAt().toString())
                .updatedAt(payment.getUpdatedAt().toString())
                .build();
        return ResponseEntity.ok(res);
    }
}
