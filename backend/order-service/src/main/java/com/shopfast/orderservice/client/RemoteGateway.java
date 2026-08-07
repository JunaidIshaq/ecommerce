package com.shopfast.orderservice.client;

import com.shopfast.common.events.CartItemDto;
import com.shopfast.common.events.CouponRedeemRequestDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.CouponValidateResponseDto;
import com.shopfast.orderservice.dto.PaymentResponseDto;
import com.shopfast.orderservice.exception.RemoteServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single entry point for every synchronous call order-service makes to another
 * service, wrapped in a circuit breaker.
 *
 * <p>Fallback policy is deliberately <em>not</em> uniform:</p>
 * <ul>
 *   <li><b>Money-affecting calls</b> (payment, coupon validation) fail loudly.
 *       Silently "succeeding" would either confirm an unpaid order or drop a
 *       discount the customer is entitled to — both are worse than an error.</li>
 *   <li><b>Best-effort calls</b> (clearing the cart after a paid order) degrade
 *       quietly, because the money has already moved and a stale cart is a
 *       cosmetic problem that a later retry can fix.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteGateway {

    private final CartClient cartClient;
    private final CouponClient couponClient;
    private final PaymentClient paymentClient;

    // ------------------------------------------------------------------ cart

    /**
     * @param buyerId the Keycloak subject for a signed-in shopper, or the anonymous
     *                browser id for a guest
     * @param guest   selects which header identifies the basket; a guest basket is
     *                stored under a different Redis key, so getting this wrong reads
     *                an empty cart and fails checkout with "Cart is empty"
     */
    @CircuitBreaker(name = "cart-service", fallbackMethod = "getCartFallback")
    @Retry(name = "cart-service")
    public List<CartItemDto> getCart(String buyerId, boolean guest) {
        return guest
                ? cartClient.getCartInternal(null, buyerId)
                : cartClient.getCartInternal(buyerId, null);
    }

    public List<CartItemDto> getCart(String userId) {
        return getCart(userId, false);
    }

    @SuppressWarnings("unused") // referenced by name from @CircuitBreaker
    private List<CartItemDto> getCartFallback(String buyerId, boolean guest, Throwable t) {
        // Reading the cart gates the whole checkout; guessing "empty" here would
        // silently discard the customer's basket, so surface the outage instead.
        throw new RemoteServiceUnavailableException("cart-service", t);
    }

    /**
     * Best effort: called only after payment has already settled.
     */
    @CircuitBreaker(name = "cart-service", fallbackMethod = "clearCartFallback")
    public void clearCart(String buyerId, boolean guest) {
        if (guest) {
            cartClient.clearCartInternal(null, buyerId);
        } else {
            cartClient.clearCartInternal(buyerId, null);
        }
    }

    public void clearCart(String userId) {
        clearCart(userId, false);
    }

    @SuppressWarnings("unused")
    private void clearCartFallback(String buyerId, boolean guest, Throwable t) {
        log.warn("Could not clear cart for buyer {} after checkout: {}. "
                + "Order is unaffected; cart will be reconciled on next write.", buyerId, t.toString());
    }

    // ---------------------------------------------------------------- coupon

    @CircuitBreaker(name = "coupon-service", fallbackMethod = "validateCouponFallback")
    @Retry(name = "coupon-service")
    public CouponValidateResponseDto validateCoupon(CouponValidateRequestDto request) {
        return couponClient.validate(request);
    }

    @SuppressWarnings("unused")
    private CouponValidateResponseDto validateCouponFallback(CouponValidateRequestDto request, Throwable t) {
        // Treating an outage as "coupon invalid" would charge the customer full
        // price for a valid coupon. Fail the checkout instead.
        throw new RemoteServiceUnavailableException("coupon-service", t);
    }

    @CircuitBreaker(name = "coupon-service", fallbackMethod = "redeemCouponFallback")
    @Retry(name = "coupon-service")
    public void redeemCoupon(CouponRedeemRequestDto request) {
        couponClient.redeem(request);
    }

    @SuppressWarnings("unused")
    private void redeemCouponFallback(CouponRedeemRequestDto request, Throwable t) {
        // Redemption runs post-payment from the Kafka consumer. Rethrowing lets the
        // listener's error handler retry and ultimately route to the DLT, so an
        // un-decremented coupon is visible rather than lost.
        throw new RemoteServiceUnavailableException("coupon-service", t);
    }

    // --------------------------------------------------------------- payment

    @CircuitBreaker(name = "payment-service", fallbackMethod = "processPaymentFallback")
    public PaymentResponseDto processPayment(String userId, PaymentRequest request) {
        return paymentClient.processPayment(userId, request);
    }

    @SuppressWarnings("unused")
    private PaymentResponseDto processPaymentFallback(String userId, PaymentRequest request, Throwable t) {
        // Never retried automatically: a timeout does not prove the charge failed,
        // and a blind retry can double-charge. Fail and let the webhook settle it.
        throw new RemoteServiceUnavailableException("payment-service", t);
    }
}
