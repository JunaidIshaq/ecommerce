package com.shopfast.paymentservice.service;

import com.shopfast.paymentservice.config.StripeProperties;
import com.shopfast.paymentservice.dto.StripePaymentIntentRequest;
import com.shopfast.paymentservice.dto.StripePaymentIntentResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    private final StripeProperties stripeProperties;

    public StripeService(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    public StripePaymentIntentResponse createPaymentIntent(StripePaymentIntentRequest request) throws StripeException {
        log.info("Creating Stripe PaymentIntent for order: {}, amount: {}", 
            request.getMetadataOrderId(), request.getAmount());

        // Build metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", request.getMetadataOrderId());
        metadata.put("userId", request.getMetadataUserId());

        // Create payment intent parameters
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency() != null ? request.getCurrency() : "usd")
                .setDescription(request.getDescription())
                .addPaymentMethodType("card")
                .putAllMetadata(metadata)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                .build();

        // Create payment intent with Stripe
        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("Stripe PaymentIntent created: {}", paymentIntent.getId());

        return StripePaymentIntentResponse.builder()
                .id(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .status(paymentIntent.getStatus())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency())
                .build();
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Retrieving Stripe PaymentIntent: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public StripePaymentIntentResponse convertToResponse(PaymentIntent paymentIntent) {
        return StripePaymentIntentResponse.builder()
                .id(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .status(paymentIntent.getStatus())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency())
                .build();
    }
}
