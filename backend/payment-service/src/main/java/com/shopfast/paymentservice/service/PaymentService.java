package com.shopfast.paymentservice.service;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.dto.StripePaymentIntentRequest;
import com.shopfast.paymentservice.enums.PaymentStatus;
import com.shopfast.paymentservice.events.KafkaPaymentProducer;
import com.shopfast.paymentservice.idempotency.RedisPaymentIdempotencyStore;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.repository.PaymentRepository;
import com.shopfast.paymentservice.repository.ProcessedCommandRepository;
import com.shopfast.paymentservice.service.StripeService;
import com.stripe.exception.StripeException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaPaymentProducer kafkaPaymentProducer;
    private final RedisPaymentIdempotencyStore idempotencyStore;
    private final StripeService stripeService;

    public PaymentService(PaymentRepository paymentRepository, 
                         ProcessedCommandRepository processedCommandRepository, 
                         KafkaPaymentProducer kafkaPaymentProducer, 
                         RedisPaymentIdempotencyStore idempotencyStore,
                         StripeService stripeService) {
        this.paymentRepository = paymentRepository;
        this.kafkaPaymentProducer = kafkaPaymentProducer;
        this.idempotencyStore = idempotencyStore;
        this.stripeService = stripeService;
    }

    @Transactional
    public Payment processPayment(PaymentRequestDto paymentRequestDto) throws StripeException {
        String orderIdStr = paymentRequestDto.getOrderId().toString();

        // 1) If an idempotency token already exists, return existing payment for that order
        boolean claimed = idempotencyStore.tryClaim(orderIdStr);
        if (!claimed) {
            return paymentRepository.findByOrderId(paymentRequestDto.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("Payment already in progress or processed for order " + orderIdStr));
        }

        try {
            // 1. Create payment record (INITIATED)
            Payment payment = Payment.builder()
                    .orderId(paymentRequestDto.getOrderId())
                    .userId(paymentRequestDto.getUserId())
                    .amount(paymentRequestDto.getAmount())
                    .method(paymentRequestDto.getMethod())
                    .status(PaymentStatus.INITIATED)
                    .build();

            Payment saved = paymentRepository.save(payment);

            // 2. Process based on payment method
            boolean success;
            String transactionId;
            
            if (paymentRequestDto.getMethod() == com.shopfast.paymentservice.enums.PaymentMethod.STRIPE) {
                // For Stripe, create PaymentIntent
                try {
                    StripePaymentIntentRequest stripeRequest = StripePaymentIntentRequest.builder()
                            .amount((long) (paymentRequestDto.getAmount() * 100)) // Convert to cents
                            .currency("usd")
                            .description("Order payment for " + orderIdStr)
                            .metadataOrderId(orderIdStr)
                            .metadataUserId(paymentRequestDto.getUserId().toString())
                            .build();

                    var paymentIntentResponse = stripeService.createPaymentIntent(stripeRequest);
                    
                    // Update payment with Stripe details
                    saved.setPaymentIntentId(paymentIntentResponse.getId());
                    saved.setClientSecret(paymentIntentResponse.getClientSecret());
                    saved.setStatus(PaymentStatus.PENDING);
                    saved.setTransactionId(paymentIntentResponse.getId());
                    paymentRepository.save(saved);
                    
                    // For Stripe, we don't determine success here - webhook will update status
                    success = true; // Return success to indicate PaymentIntent created
                    transactionId = paymentIntentResponse.getId();
                    
                    log.info("Stripe PaymentIntent created for payment: {}, clientSecret: {}", 
                        saved.getId(), paymentIntentResponse.getClientSecret());
                } catch (Exception e) {
                    log.error("Failed to create Stripe PaymentIntent", e);
                    saved.setStatus(PaymentStatus.FAILED);
                    saved.setTransactionId("STRIPE_ERROR-" + UUID.randomUUID());
                    paymentRepository.save(saved);
                    throw e;
                }
            } else {
                // For other payment methods (CARD, PAYPAL, COD), use mock gateway
                success = mockPaymentGateway(saved);
                transactionId = success ? "TXN-" + UUID.randomUUID() : "TXN-FAILED" + UUID.randomUUID();
                saved.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
                saved.setTransactionId(transactionId);
                paymentRepository.save(saved);
            }

            // 3. Publish payment event (only for non-pending statuses)
            if (paymentRequestDto.getMethod() != com.shopfast.paymentservice.enums.PaymentMethod.STRIPE || success) {
                PaymentEvent event = new PaymentEvent();
                event.setEventId(UUID.randomUUID().toString());
                event.setEventType(saved.getStatus() == PaymentStatus.SUCCESS ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
                event.setEventVersion(1);
                event.setOccurredAt(Instant.now());
                event.setPayload(Map.of(
                        "paymentId", saved.getId().toString(),
                        "orderId", saved.getOrderId().toString(),
                        "userId", saved.getUserId().toString(),
                        "amount", saved.getAmount(),
                        "status", saved.getStatus().name(),
                        "transactionId", saved.getTransactionId()
                ));

                kafkaPaymentProducer.publish(event);
            }

            log.info("✅ Payment {} processed with method {}", saved.getId(), paymentRequestDto.getMethod());
            return saved;
        } catch (Exception ex) {
            idempotencyStore.clear(orderIdStr);
            throw ex;
        }
    }

    // Very simple mock: succeed 90% of the time for non-Stripe methods
    private boolean mockPaymentGateway(Payment payment) {
        double rand = Math.random();
        return rand < 0.90;
    }

    public Payment getById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    public void updatePaymentFromWebhook(String paymentIntentId, String status, String transactionId) {
        paymentRepository.findByPaymentIntentId(paymentIntentId).ifPresentOrElse(
            payment -> {
                payment.setStatus(PaymentStatus.valueOf(status));
                if (transactionId != null) {
                    payment.setTransactionId(transactionId);
                }
                paymentRepository.save(payment);
                log.info("Updated payment {} status to {} via webhook", payment.getId(), status);
            },
            () -> log.warn("Payment not found for paymentIntentId: {}", paymentIntentId)
        );
    }
}
