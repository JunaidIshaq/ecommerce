package com.shopfast.orderservice.service;

import com.shopfast.common.events.CartItemDto;
import com.shopfast.orderservice.dto.OrderItemDto;
import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.events.KafkaNotificationProducer;
import com.shopfast.orderservice.events.KafkaOrderProducer;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import com.shopfast.orderservice.util.OrderNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedCommandRepository processedCommandRepository;

    @Mock
    private KafkaOrderProducer kafkaOrderProducer;

    @Mock
    private KafkaNotificationProducer kafkaNotificationProducer;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDto sampleRequest() {
        OrderItemDto item = OrderItemDto.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        OrderRequestDto request = new OrderRequestDto();
        request.setUserId("user-1");
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void placeOrderComputesTotalsAndPublishesEvents() {
        when(orderNumberGenerator.next()).thenReturn("ORD-20260101-ABCDEFGHIJKL");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.placeOrder(sampleRequest());

        assertThat(order.getSubTotal()).isEqualByComparingTo("20.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(kafkaOrderProducer).reserveOrder(any());
        verify(kafkaNotificationProducer).send(any());
    }

    @Test
    void requireOrderByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.requireOrderById(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void confirmOrderSetsConfirmedAndPublishes() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.confirmOrder(id);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(kafkaOrderProducer).confirmOrder(any());
    }

    @Test
    void cancelOrderRejectsConfirmedOrders() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel confirmed order");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderSucceedsForPending() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(id);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(kafkaOrderProducer).releaseOrder(any());
    }

    @Test
    void handleInventoryEventAppliesReservedStatus() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.handleInventoryEvent("corr", "INVENTORY_RESERVED",
                Map.of("correlationId", id.toString()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void handleInventoryEventIgnoresMalformedOrderId() {
        orderService.handleInventoryEvent("corr", "INVENTORY_RESERVED", Map.of("correlationId", "bad"));

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void handleInventoryEventIgnoresUnhandledEventType() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.of(Order.builder().id(id).status(OrderStatus.PENDING).build()));

        orderService.handleInventoryEvent("corr", "SOMETHING_ELSE", Map.of("correlationId", id.toString()));

        verify(orderRepository, never()).save(any());
    }
}
