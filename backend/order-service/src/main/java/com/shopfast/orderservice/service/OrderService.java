package com.shopfast.orderservice.service;

import com.shopfast.common.events.CartItemDto;
import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.events.KafkaOrderProducer;
import com.shopfast.common.events.OrderCommand;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.model.ProcessedCommand;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final KafkaOrderProducer kafkaOrderProducer;
//    private final CouponClient couponClient;

    public OrderService(OrderRepository orderRepository, ProcessedCommandRepository processedCommandRepository, KafkaOrderProducer kafkaOrderProducer) {
        this.orderRepository = orderRepository;
        this.processedCommandRepository = processedCommandRepository;
        this.kafkaOrderProducer = kafkaOrderProducer;
    }

    @Transactional
    public Order placeOrder(OrderRequestDto orderRequestDto) {
        // Calculate totals
        BigDecimal total = orderRequestDto.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String orderNumber = "Order-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .userId(orderRequestDto.getUserId())
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .subTotal(total)
                .totalAmount(total)
                .build();

        List<OrderItem> items = orderRequestDto.getItems().stream().map(i -> {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(i.getProductId())
                    .quantity(i.getQuantity())
                    .price(i.getPrice())
                    .build();
            return orderItem;
        }).toList();

        order.setItems(items);
        Order saved = orderRepository.save(order);

        kafkaOrderProducer.reserveOrder(order);

        return saved;
    }

    public List<Order> getOrdersForUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public Optional<Order> getOrderById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public Order confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        kafkaOrderProducer.confirmOrder(order);
        return order;
    }

    @Transactional
    public Order cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        if(order.getStatus() == OrderStatus.CONFIRMED) {
            throw new  IllegalStateException("Cannot cancel confirmed order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        kafkaOrderProducer.releaseOrder(order);
        return order;
    }

    // Invoked by InventoryEvent listener to mark order RESERVED/CONFIRMED/REJECTED
    @Transactional
    public void handleInventoryEvent(String commandCorrelationId, String eventType, Map<String, Object> payload) {
        //correlationId contains orderId in our design

        String orderId = (String) payload.get("correlationId");
        if(orderId == null) {
            //sometimes payload contains orderId in top-level, fallback to payload.orderId
            orderId = payload.get("orderId").toString();
        }
        if(orderId == null) {
            log.warn("InventoryEvent without orderId/correlationId, skipping");
            return;
        }
        UUID orderIdUUID = UUID.fromString(orderId);
        Order order = orderRepository.findById(orderIdUUID).orElse(null);
        if(order == null) {
            log.warn("Order {} not found for inventory event", orderId);
            return;
        }

        switch(eventType) {
            case "INVENTORY_RESERVED" -> {
                order.setStatus(OrderStatus.RESERVED);
            }
            case "INVENTORY_CONFIRMED" -> {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            case "INVENTORY_FAILED" -> {
                order.setStatus(OrderStatus.REJECTED);
            }
            case "INVENTORY_RELEASED" -> {
                order.setStatus(OrderStatus.CANCELLED);
            }
        }
        orderRepository.save(order);
    }

    @Transactional
    public Object createFromCart(String userId, List<CartItemDto> items, String couponCode) {
//        double subtotal = items.stream().mapToDouble(item -> item.getPrice().doubleValue() * item.getQuantity()).sum();
//        double discount = (couponCode != null) ? couponClient.validate(userId, couponCode, items, subtotal).discount() : 0.0;
//        double total = Math.max(0, subtotal - discount);
//
//        var order = saveOrder(userId, items, subtotal, discount, total);
//        // public reserve command
//        // Publish RESERVE command for each item (or aggregated payload)
//        String commandId = UUID.randomUUID().toString();
//        OrderCommand orderCommand = new OrderCommand();
//        orderCommand.setCommandId(commandId);
//        orderCommand.setCommandType("RESERVE");
//        orderCommand.setOccurredAt(Instant.now());
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("orderId", order.getId().toString());
//        payload.put("userId", order.getUserId());
//
//        // Include Items List
//        payload.put("items", order.getItems().stream().map(i -> Map.of(
//                "productId", i.getProductId().toString(),
//                "quantity", i.getQuantity()
//        )).toList());
//        orderCommand.setPayload(payload);
//
//        // Persist processed command to avoid re-processing later (optional)
//        processedCommandRepository.save(ProcessedCommand.builder()
//                .commandId(commandId)
//                .processedAt(Instant.now())
//                .build());
//
//        kafkaOrderProducer.publishOrderCommand(orderCommand);
//        return order;
        return null;
    }
}
