package com.shopfast.orderservice.domain;

import com.shopfast.orderservice.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Allow-list of legal order status transitions.
 *
 * <p>Kafka delivers at-least-once and without cross-partition ordering guarantees, so a
 * stale {@code PAYMENT_FAILED} can arrive after a {@code PAYMENT_SUCCESS}. Without a guard
 * the later event silently regresses a confirmed order. Consumers must call
 * {@link #isAllowed} (or {@link #canApply}) before mutating status.</p>
 */
@Slf4j
public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.CREATED, EnumSet.of(
                OrderStatus.PENDING, OrderStatus.RESERVED, OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.PAYMENT_FAILED));
        ALLOWED.put(OrderStatus.PENDING, EnumSet.of(
                OrderStatus.RESERVED, OrderStatus.CONFIRMED, OrderStatus.CANCELLED,
                OrderStatus.REJECTED, OrderStatus.PAYMENT_FAILED));
        ALLOWED.put(OrderStatus.RESERVED, EnumSet.of(
                OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.REJECTED,
                OrderStatus.PAYMENT_FAILED));
        ALLOWED.put(OrderStatus.CONFIRMED, EnumSet.of(
                OrderStatus.REFUNDED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.PAYMENT_FAILED, EnumSet.of(
                OrderStatus.CANCELLED, OrderStatus.CONFIRMED)); // retry may later succeed
        // Terminal states
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusTransitions() {
    }

    public static boolean isAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true; // idempotent replay
        }
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }

    /**
     * Logs and returns {@code false} instead of throwing, so an out-of-order event can be
     * acknowledged and discarded rather than poisoning the partition.
     */
    public static boolean canApply(Object orderRef, OrderStatus from, OrderStatus to) {
        if (isAllowed(from, to)) {
            return true;
        }
        log.warn("Ignoring illegal order status transition {} -> {} for {}", from, to, orderRef);
        return false;
    }
}
