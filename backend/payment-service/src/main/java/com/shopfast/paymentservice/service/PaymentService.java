package com.shopfast.paymentservice.service;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.dto.StripePaymentIntentRequest;
import com.shopfast.paymentservice.enums.PaymentMethod;
import com.shopfast.paymentservice.enums.PaymentStatus;
import com.shopfast.paymentservice.events.KafkaPaymentProducer;
import com.shopfast.paymentservice.idempotency.RedisPaymentIdempotencyStore;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.repository.PaymentRepository;
import com.shopfast.paymentservice.repository.ProcessedCommandRepository;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Mock card processing is a development aid only. It must never be reachable in an
     * environment that handles real money, so it is opt-in via configuration and defaults
     * to OFF.
     */
    @Value("${payment.mock-gateway.enabled:false}")
    private boolean mockGatewayEnabled;

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
            // Validate card details if payment method is CARD
            if (paymentRequestDto.getMethod() == PaymentMethod.CARD) {
                if (!validateCardDetails(paymentRequestDto)) {
                    throw new IllegalArgumentException("Invalid card details");
                }
            }

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

            if (paymentRequestDto.getMethod() == PaymentMethod.STRIPE) {
                // For Stripe, create PaymentIntent
                try {
                    StripePaymentIntentRequest stripeRequest = StripePaymentIntentRequest.builder()
                            .amount(toMinorUnits(paymentRequestDto.getAmount())) // Convert to cents
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

                    // Stripe is asynchronous: the intent exists but no money has moved yet.
                    // The webhook is the only authority on success, so nothing is emitted here.
                    success = false;
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
            } else if (paymentRequestDto.getMethod() == PaymentMethod.COD) {
                // Cash on Delivery - always success, payment will be collected on delivery
                success = true;
                transactionId = "COD-" + UUID.randomUUID();
                saved.setStatus(PaymentStatus.SUCCESS);
                saved.setTransactionId(transactionId);
                paymentRepository.save(saved);
                log.info("COD payment recorded for order: {}", orderIdStr);
            } else {
                // For CARD (mock), validate and process
                if (!mockGatewayEnabled) {
                    throw new IllegalStateException(
                            "Card payments are not available: no real gateway is configured and the mock "
                                    + "gateway is disabled (payment.mock-gateway.enabled=false)");
                }
                success = mockPaymentGateway(saved);
                transactionId = success ? "TXN-" + UUID.randomUUID() : "TXN-FAILED-" + UUID.randomUUID();
                saved.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
                saved.setTransactionId(transactionId);
                paymentRepository.save(saved);
            }

            // 3. Publish only terminal outcomes. PENDING/INITIATED payments must not be
            //    broadcast, otherwise the order saga would read them as failures.
            if (saved.getStatus() == PaymentStatus.SUCCESS || saved.getStatus() == PaymentStatus.FAILED) {
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

            log.info("Payment {} processed with method {}", saved.getId(), paymentRequestDto.getMethod());
            return saved;
        } catch (Exception ex) {
            idempotencyStore.clear(orderIdStr);
            throw ex;
        }
    }

    private boolean validateCardDetails(PaymentRequestDto dto) {
        if (dto.getCardNumber() == null || dto.getCardNumber().replaceAll("\\s", "").length() < 15) {
            log.warn("Invalid card number length");
            return false;
        }
        if (dto.getCardHolderName() == null || dto.getCardHolderName().trim().length() < 2) {
            log.warn("Invalid card holder name");
            return false;
        }
        if (dto.getExpiryDate() == null || !dto.getExpiryDate().matches("\\d{2}/\\d{2}")) {
            log.warn("Invalid expiry date format");
            return false;
        }
        if (dto.getCvv() == null || dto.getCvv().length() < 3) {
            log.warn("Invalid CVV");
            return false;
        }
        return true;
    }

    // Very simple mock: succeed 90% of the time for non-Stripe methods
    private boolean mockPaymentGateway(Payment payment) {
        double rand = Math.random();
        return rand < 0.90;
    }

    /** Converts a major-unit amount to Stripe's integer minor units without truncating cents. */
    private long toMinorUnits(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    public Payment getById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    @Transactional
    public void updatePaymentFromWebhook(String paymentIntentId, String status, String transactionId) {
        // Webhook payloads are attacker-reachable input; an unknown status must be rejected
        // cleanly instead of blowing up with IllegalArgumentException from valueOf.
        final PaymentStatus newStatus;
        try {
            newStatus = PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Ignoring webhook for intent {} with unknown status '{}'", paymentIntentId, status);
            return;
        }
        paymentRepository.findByPaymentIntentId(paymentIntentId).ifPresentOrElse(
            payment -> {
                if (payment.getStatus() == newStatus) {
                    log.debug("Webhook replay ignored for payment {} (already {})", payment.getId(), newStatus);
                    return;
                }
                payment.setStatus(newStatus);
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
