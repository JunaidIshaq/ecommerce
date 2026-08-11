package com.shopfast.orderservice.domain;

import com.shopfast.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTransitionsTest {

    @Test
    void pendingCanTransitionToReservedConfirmedCancelledRejected() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.RESERVED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.REJECTED)).isTrue();
    }

    @Test
    void reservedCannotRegressToPending() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.RESERVED, OrderStatus.PENDING)).isFalse();
    }

    @Test
    void confirmedCanOnlyRefundOrCancel() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CONFIRMED, OrderStatus.PENDING)).isFalse();
    }

    @Test
    void terminalStatesAcceptNoTransitions() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.REJECTED, OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.REFUNDED, OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void sameStateIsIdempotentReplay() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, OrderStatus.PENDING)).isTrue();
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThat(OrderStatusTransitions.isAllowed(null, OrderStatus.PENDING)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING, null)).isFalse();
    }

    @Test
    void canApplyLogsAndReturnsFalseForIllegalTransition() {
        assertThat(OrderStatusTransitions.canApply("order-1", OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatusTransitions.canApply("order-1", OrderStatus.PENDING, OrderStatus.RESERVED)).isTrue();
    }
}
