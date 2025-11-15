package com.shopfast.orderservice.service;

import com.shopfast.common.events.CartItemDto;
import com.shopfast.common.events.CouponLineItemDto;
import com.shopfast.common.events.CouponValidateRequestDto;
import com.shopfast.common.events.OrderCommand;
import com.shopfast.orderservice.client.CartClient;
import com.shopfast.orderservice.client.CouponClient;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.events.KafkaOrderProducer;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.OrderItem;
import com.shopfast.orderservice.model.ProcessedCommand;
import com.shopfast.orderservice.repository.OrderItemRepository;
import com.shopfast.orderservice.repository.OrderRepository;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.configuration.AbstractFileConfiguration;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartClient cartClient;
    private final KafkaOrderProducer kafkaOrderProducer;
    private final ProcessedCommandRepository processedCommandRepository;
    private final CouponClient couponClient;

    public CheckoutService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, CartClient cartClient, KafkaOrderProducer kafkaOrderProducer, ProcessedCommandRepository processedCommandRepository, CouponClient couponClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartClient = cartClient;
        this.kafkaOrderProducer = kafkaOrderProducer;
        this.processedCommandRepository = processedCommandRepository;
        this.couponClient = couponClient;
    }

    @Transactional
    public Order checkout (String userId, String couponCode) {
        // 1) Load Cart
        var cartItems = cartClient.getCartInternal(userId);
        if(cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // 2) Price calculation (basic; coupons integrated in step 3)
        double subTotal = cartItems.stream().mapToDouble(i -> i.getPrice().doubleValue() * i.getQuantity())
                .sum();
        double discount = 0.0;
        double total = Math.max(9, subTotal - discount);

        if(couponCode != null && !couponCode.isBlank()) {
            CouponValidateRequestDto requestDto = new CouponValidateRequestDto();
            requestDto.setUserId(userId);
            requestDto.setCode(couponCode);
            requestDto.setSubTotal(subTotal);
            requestDto.setItems(cartItems.stream().map(ci -> {
                CouponLineItemDto couponLineItemDto = new CouponLineItemDto();
                couponLineItemDto.setProductId(ci.getProductId().toString());
                couponLineItemDto.setQuantity(ci.getQuantity());
                couponLineItemDto.setPrice(ci.getPrice().doubleValue());
                return couponLineItemDto;
            }).toList());
            var response = couponClient.validate(requestDto);
            if(response.isValid()) {
                discount = response.getDiscount();
                total = Math.max(total, subTotal - discount);
            }else {
                throw new IllegalArgumentException("Coupon code is invalid");
            }
        }

        // 3) Persist order + items
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setSubTotal(BigDecimal.valueOf(subTotal));
        order.setDiscount(BigDecimal.valueOf(discount));
        order.setTotalAmount(BigDecimal.valueOf(total));
        order = orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();
        for(CartItemDto cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            items.add(orderItem);
        }

        orderItemRepository.saveAll(items);

        // 4) Publish RESERVE command for each item (or aggregated payload)
        String commandId = UUID.randomUUID().toString();
        OrderCommand orderCommand = new OrderCommand();
        orderCommand.setCommandId(commandId);
        orderCommand.setCommandType("RESERVE");
        orderCommand.setOccurredAt(Instant.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("userId", order.getUserId());

        // Include Items List
        payload.put("items", order.getItems().stream().map(i -> Map.of(
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

        // 5) Move to CREATED (Inventory will update to RESERVED via events listener you already have)
        return order;
    }
}
