package com.shopfast.paymentservice.service;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.enums.PaymentStatus;
import com.shopfast.paymentservice.events.KafkaPaymentProducer;
import com.shopfast.paymentservice.idempotency.RedisPaymentIdempotencyStore;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.repository.PaymentRepository;
import com.shopfast.paymentservice.repository.ProcessedCommandRepository;
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

    public PaymentService(PaymentRepository paymentRepository, ProcessedCommandRepository processedCommandRepository, KafkaPaymentProducer kafkaPaymentProducer, RedisPaymentIdempotencyStore idempotencyStore) {
        this.paymentRepository = paymentRepository;
        this.kafkaPaymentProducer = kafkaPaymentProducer;
        this.idempotencyStore = idempotencyStore;
    }

    @Transactional
    public Payment processPayment(PaymentRequestDto paymentRequestDto) {

        String orderIdStr = paymentRequestDto.getOrderId().toString();

        // 1) If an idempotency token already exists, return existing payment for that order (if present)
        boolean claimed = idempotencyStore.tryClaim(orderIdStr);
        if (!claimed) {
            // Duplicate attempt — attempt to return existing payment record
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

            // 2. Call mock gateway (simulate)

            boolean success = mockPaymentGateway(saved);

            // 3. Update based on result
            if (success) {
                saved.setStatus(PaymentStatus.SUCCESS);
                saved.setTransactionId("TXN-" + UUID.randomUUID());
            } else {
                saved.setStatus(PaymentStatus.FAILED);
                saved.setTransactionId("TXN-FAILED" + UUID.randomUUID());
            }
            paymentRepository.save(saved);

            // 4. Publish payment event
            PaymentEvent event = new PaymentEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType(success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
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

            log.info("✅ Payment {} successful and event published", payment.getId());
            return saved;
        } catch (Exception ex) {
            // On unexpected exception: clear idempotency token so client may retry
            idempotencyStore.clear(orderIdStr);
            throw ex;
        }
    }

    //very simple mock: succeed 90% of the time
    private boolean mockPaymentGateway(Payment payment) {
        double rand = Math.random();
        return rand < 0.90;
    }

    public Payment getById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }
}
