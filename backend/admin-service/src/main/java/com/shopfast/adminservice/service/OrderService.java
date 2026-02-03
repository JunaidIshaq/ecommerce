//package com.shopfast.adminservice.service;
//
//import com.shopfast.common.enums.NotificationChannel;
//import com.shopfast.common.enums.NotificationType;
//import com.shopfast.common.events.CartItemDto;
//import com.shopfast.common.events.NotificationEvent;
//import com.shopfast.adminservice.dto.OrderRequestDto;
//import com.shopfast.adminservice.enums.OrderStatus;
//import com.shopfast.adminservice.events.KafkaNotificationProducer;
//import com.shopfast.adminservice.events.KafkaOrderProducer;
//import com.shopfast.adminservice.model.Order;
//import com.shopfast.adminservice.model.OrderItem;
//import com.shopfast.adminservice.repository.OrderRepository;
//import com.shopfast.adminservice.repository.ProcessedCommandRepository;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//import java.util.NoSuchElementException;
//import java.util.Optional;
//import java.util.UUID;
//
//@Slf4j
//@Service
//public class OrderService {
//
//    private final OrderRepository orderRepository;
//    private final ProcessedCommandRepository processedCommandRepository;
//    private final KafkaOrderProducer kafkaOrderProducer;
//    private final KafkaNotificationProducer kafkaNotificationProducer;
////    private final CouponAdminClient couponClient;
//
//    public OrderService(OrderRepository orderRepository, ProcessedCommandRepository processedCommandRepository, KafkaOrderProducer kafkaOrderProducer, KafkaNotificationProducer kafkaNotificationProducer) {
//        this.orderRepository = orderRepository;
//        this.processedCommandRepository = processedCommandRepository;
//        this.kafkaOrderProducer = kafkaOrderProducer;
//        this.kafkaNotificationProducer = kafkaNotificationProducer;
//    }
//
//    @Transactional
//    public Order placeOrder(OrderRequestDto orderRequestDto) {
//        // Calculate totals
//        BigDecimal total = orderRequestDto.getItems().stream()
//                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        String orderNumber = "Order-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//
//        Order order = Order.builder()
//                .userId(orderRequestDto.getUserId())
//                .orderNumber(orderNumber)
//                .status(OrderStatus.PENDING)
//                .subTotal(total)
//                .totalAmount(total)
//                .build();
//
//        List<OrderItem> items = orderRequestDto.getItems().stream().map(i -> {
//            OrderItem orderItem = OrderItem.builder()
//                    .order(order)
//                    .productId(i.getProductId())
//                    .quantity(i.getQuantity())
//                    .price(i.getPrice())
//                    .build();
//            return orderItem;
//        }).toList();
//
//        order.setItems(items);
//        Order saved = orderRepository.save(order);
//
//        // Order Producer -> Inventory Service
//        kafkaOrderProducer.reserveOrder(order);
//
//        // Notification Producer -> Notification Service
//        NotificationEvent notificationEvent = new NotificationEvent();
//        notificationEvent.setSubject("Order Created");
//        notificationEvent.setNotificationType(NotificationType.ORDER_CREATED);
//        notificationEvent.setNotificationChannel(NotificationChannel.EMAIL);
//        notificationEvent.setContent("Order has been placed");
//        notificationEvent.setRecipient("junaidnumlcs@gmail.com");
//        notificationEvent.setReferenceId(order.getOrderNumber());
//        notificationEvent.setUserId("28e2ac7f-09ef-4e7e-94df-042a987fa9c9");
//        notificationEvent.setEventSource("order-service");
//        kafkaNotificationProducer.send(notificationEvent);
//
//        return saved;
//    }
//
//    public List<Order> getOrdersForUser(String userId) {
//        return orderRepository.findByUserId(userId);
//    }
//
//    public Optional<Order> getOrderById(UUID orderId) {
//        return orderRepository.findById(orderId);
//    }
//
//    @Transactional
//    public Order confirmOrder(UUID orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new NoSuchElementException("Order not found"));
//        order.setStatus(OrderStatus.CONFIRMED);
//        order = orderRepository.save(order);
//
//        kafkaOrderProducer.confirmOrder(order);
//        return order;
//    }
//
//    @Transactional
//    public Order cancelOrder(UUID orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new NoSuchElementException("Order not found"));
//        if(order.getStatus() == OrderStatus.CONFIRMED) {
//            throw new  IllegalStateException("Cannot cancel confirmed order");
//        }
//        order.setStatus(OrderStatus.CANCELLED);
//        order = orderRepository.save(order);
//
//        kafkaOrderProducer.releaseOrder(order);
//        return order;
//    }
//
//    // Invoked by InventoryEvent listener to mark order RESERVED/CONFIRMED/REJECTED
//    @Transactional
//    public void handleInventoryEvent(String commandCorrelationId, String eventType, Map<String, Object> payload) {
//        //correlationId contains orderId in our design
//
//        String orderId = (String) payload.get("correlationId");
//        if(orderId == null) {
//            //sometimes payload contains orderId in top-level, fallback to payload.orderId
//            orderId = payload.get("orderId").toString();
//        }
//        if(orderId == null) {
//            log.warn("InventoryEvent without orderId/correlationId, skipping");
//            return;
//        }
//        UUID orderIdUUID = UUID.fromString(orderId);
//        Order order = orderRepository.findById(orderIdUUID).orElse(null);
//        if(order == null) {
//            log.warn("Order {} not found for inventory event", orderId);
//            return;
//        }
//
//        switch(eventType) {
//            case "INVENTORY_RESERVED" -> {
//                order.setStatus(OrderStatus.RESERVED);
//            }
//            case "INVENTORY_CONFIRMED" -> {
//                order.setStatus(OrderStatus.CONFIRMED);
//            }
//            case "INVENTORY_FAILED" -> {
//                order.setStatus(OrderStatus.REJECTED);
//            }
//            case "INVENTORY_RELEASED" -> {
//                order.setStatus(OrderStatus.CANCELLED);
//            }
//        }
//        orderRepository.save(order);
//    }
//
//    @Transactional
//    public Object createFromCart(String userId, List<CartItemDto> items, String couponCode) {
////        double subtotal = items.stream().mapToDouble(item -> item.getPrice().doubleValue() * item.getQuantity()).sum();
////        double discount = (couponCode != null) ? couponClient.validate(userId, couponCode, items, subtotal).discount() : 0.0;
////        double total = Math.max(0, subtotal - discount);
////
////        var order = saveOrder(userId, items, subtotal, discount, total);
////        // public reserve command
////        // Publish RESERVE command for each item (or aggregated payload)
////        String commandId = UUID.randomUUID().toString();
////        OrderCommand orderCommand = new OrderCommand();
////        orderCommand.setCommandId(commandId);
////        orderCommand.setCommandType("RESERVE");
////        orderCommand.setOccurredAt(Instant.now());
////        Map<String, Object> payload = new HashMap<>();
////        payload.put("orderId", order.getId().toString());
////        payload.put("userId", order.getUserId());
////
////        // Include Items List
////        payload.put("items", order.getItems().stream().map(i -> Map.of(
////                "productId", i.getProductId().toString(),
////                "quantity", i.getQuantity()
////        )).toList());
////        orderCommand.setPayload(payload);
////
////        // Persist processed command to avoid re-processing later (optional)
////        processedCommandRepository.save(ProcessedCommand.builder()
////                .commandId(commandId)
////                .processedAt(Instant.now())
////                .build());
////
////        kafkaOrderProducer.publishOrderCommand(orderCommand);
////        return order;
//        return null;
//    }
//
//// TODO - Send Notifications for ORDER CREATED, APPROVED, SHIPPED, DELIVERED, REJECTED
////    public void createOrderNotification(Order order, User user) {
////        NotificationEvent event = new NotificationEvent();
////        event.setUserId(user.getId());
////        event.setRecipient(user.getEmail());
////        event.setNotificationType(NotificationType.ORDER_CREATED);
////        event.setNotificationChannel(NotificationChannel.EMAIL);
////        event.setSubject("Your order has been placed");
////        event.setContent("Your order #" + order.getId() + " has been successfully placed.");
////        event.setEventSource("order-service");
////        event.setReferenceId(order.getId().toString());
////
////        kafkaNotificationProducer.send(event);
////    }
//}
