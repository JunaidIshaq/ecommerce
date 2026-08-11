package com.shopfast.paymentservice.service;

import com.shopfast.common.events.PaymentEvent;
import com.shopfast.paymentservice.dto.PaymentRequestDto;
import com.shopfast.paymentservice.enums.PaymentMethod;
import com.shopfast.paymentservice.enums.PaymentStatus;
import com.shopfast.paymentservice.events.KafkaPaymentProducer;
import com.shopfast.paymentservice.idempotency.RedisPaymentIdempotencyStore;
import com.shopfast.paymentservice.model.Payment;
import com.shopfast.paymentservice.repository.PaymentRepository;
import com.shopfast.paymentservice.repository.ProcessedCommandRepository;
import com.shopfast.paymentservice.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedCommandRepository processedCommandRepository;

    @Mock
    private KafkaPaymentProducer kafkaPaymentProducer;

    @Mock
    private RedisPaymentIdempotencyStore idempotencyStore;

    @Mock
    private StripeService stripeService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, processedCommandRepository,
                kafkaPaymentProducer, idempotencyStore, stripeService);
    }

    private PaymentRequestDto codRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(UUID.randomUUID());
        request.setUserId(UUID.randomUUID());
        request.setAmount(100.0);
        request.setMethod(PaymentMethod.COD);
        return request;
    }

    private PaymentRequestDto baseRequest(PaymentMethod method) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(UUID.randomUUID());
        request.setUserId(UUID.randomUUID());
        request.setAmount(10.0);
        request.setMethod(method);
        return request;
    }

    @Test
    void processCodPaymentMarksSuccessAndPublishes() throws Exception {
        when(idempotencyStore.tryClaim(anyString())).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment pay = inv.getArgument(0);
            if (pay.getId() == null) pay.setId(java.util.UUID.randomUUID());
            return pay;
        });

        Payment payment = paymentService.processPayment(codRequest());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(kafkaPaymentProducer).publish(any(PaymentEvent.class));
        verify(idempotencyStore, never()).clear(anyString());
    }

    @Test
    void processPaymentReturnsExistingWhenIdempotencyClaimed() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequestDto request = baseRequest(PaymentMethod.COD);
        request.setOrderId(orderId);
        when(idempotencyStore.tryClaim(anyString())).thenReturn(false);
        Payment existing = Payment.builder().orderId(orderId).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        Payment result = paymentService.processPayment(request);

        assertThat(result).isEqualTo(existing);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPaymentThrowsWhenAlreadyInProgressButNoExisting() throws Exception {
        PaymentRequestDto request = codRequest();
        when(idempotencyStore.tryClaim(anyString())).thenReturn(false);
        when(paymentRepository.findByOrderId(request.getOrderId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(IllegalStateException.class);
        verify(idempotencyStore, never()).clear(anyString());
    }

    @Test
    void processCardPaymentThrowsWhenMockGatewayDisabled() throws Exception {
        PaymentRequestDto request = baseRequest(PaymentMethod.CARD);
        request.setCardNumber("4111111111111111");
        request.setCardHolderName("Jane Doe");
        request.setExpiryDate("12/29");
        request.setCvv("123");
        when(idempotencyStore.tryClaim(anyString())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mock");
        verify(idempotencyStore).clear(anyString());
    }

    @Test
    void processPaymentClearsIdempotencyOnFailure() throws Exception {
        PaymentRequestDto request = baseRequest(PaymentMethod.STRIPE);
        when(idempotencyStore.tryClaim(anyString())).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(new Payment());
        doThrow(new RuntimeException("stripe boom")).when(stripeService).createPaymentIntent(any());

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(RuntimeException.class);
        verify(idempotencyStore).clear(anyString());
    }

    @Test
    void updatePaymentFromWebhookIgnoresUnknownStatus() {
        paymentService.updatePaymentFromWebhook("pi_1", "NOT_A_STATUS", "txn");

        verify(paymentRepository, never()).findByPaymentIntentId(anyString());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(id))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
