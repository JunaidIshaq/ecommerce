package com.shopfast.orderservice.service;

import com.shopfast.orderservice.dto.OrderRequestDto;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.events.KafkaOrderProducer;
import com.shopfast.orderservice.events.OrderCommand;
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

        // Publish RESERVE command for each item (or aggregated payload)
        String commandId = UUID.randomUUID().toString();
        OrderCommand orderCommand = new OrderCommand();
        orderCommand.setCommandId(commandId);
        orderCommand.setCommandType("RESERVE");
        orderCommand.setOccurredAt(Instant.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", saved.getId().toString());
        payload.put("userId", saved.getUserId());

        // Include Items List
        payload.put("items", saved.getItems().stream().map(i -> Map.of(
                "productId", i.getProductId().toString(),
                "quantity", i.getQuantity()
        )).toList());
        orderCommand.setPayload(payload);

        // Persist processed command to avoid re-processing later (optional)
        processedCommandRepository.save(ProcessedCommand.builder()
                .commandId(commandId)
                .processedAt(Instant.now())
                .build());

        kafkaOrderProducer.publishOrderCommand(orderCommand);

        log.info("✅ Order {} placed and RELEASE command {} published", order.getId(), commandId);
        return saved;
    }

    public List<Order> getOrdersForUser(String userId) {
        return orderRepository.findByUserId(UUID.fromString(userId));
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

        //Public RELEASE command
        String commandId = UUID.randomUUID().toString();
        OrderCommand orderCommand = new OrderCommand();
        orderCommand.setCommandId(commandId);
        orderCommand.setCommandType("CONFIRMED");
        orderCommand.setOccurredAt(Instant.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        //Items
        payload.put("items", order.getItems().stream().map(i -> Map.of(
                "producedId", i.getProductId().toString(),
                "quantity", i.getQuantity()
        )).toList());

        orderCommand.setPayload(payload);

        processedCommandRepository.save(ProcessedCommand.builder()
                .commandId(commandId)
                .processedAt(Instant.now())
                .build());
        log.info("✅ Order {} confirmed and CONFIRM command {} published", orderId, commandId);
        kafkaOrderProducer.publishOrderCommand(orderCommand);
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

        //Public RELEASE command
        String commandId = UUID.randomUUID().toString();
        OrderCommand orderCommand = new OrderCommand();
        orderCommand.setCommandId(commandId);
        orderCommand.setCommandType("RELEASE");
        orderCommand.setOccurredAt(Instant.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        //Items
        payload.put("items", order.getItems().stream().map(i -> Map.of(
                "producedId", i.getProductId().toString(),
                "quantity", i.getQuantity()
        )).toList());

        orderCommand.setPayload(payload);

        processedCommandRepository.save(ProcessedCommand.builder()
                .commandId(commandId)
                .processedAt(Instant.now())
                .build());
        log.info("✅ Order {} canceled and RELEASE command {} published", orderId, commandId);
        kafkaOrderProducer.publishOrderCommand(orderCommand);
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

}
