package com.shopfast.orderservice.events;

import com.shopfast.common.events.OrderCommand;
import com.shopfast.orderservice.model.Order;
import com.shopfast.orderservice.model.ProcessedCommand;
import com.shopfast.orderservice.repository.ProcessedCommandRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final ProcessedCommandRepository processedCommandRepository;

    public KafkaOrderProducer(KafkaTemplate<String, Object> kafkaTemplate, ProcessedCommandRepository processedCommandRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.processedCommandRepository = processedCommandRepository;
    }

    private final String ORDER_COMMANDS_TOPIC = "order.commands";

    public void publishOrderCommand(OrderCommand orderCommand) {
        kafkaTemplate.send(ORDER_COMMANDS_TOPIC, orderCommand.getCommandId(), orderCommand)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        log.info("Published OrderCommand {} to partition {} with offset {}",
                                orderCommand.getCommandId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish OrderCommand {} due to {}", orderCommand.getCommandId(), error.getMessage(), error);
                    }
        });
    }

    public void releaseOrder(Order order) {
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
        log.info("✅ Order {} canceled and RELEASE command {} published", order.getId(), commandId);
        publishOrderCommand(orderCommand);
    }

    public void confirmOrder(Order order) {
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
        log.info("✅ Order {} confirmed and CONFIRM command {} published", order.getId(), commandId);
        publishOrderCommand(orderCommand);
    }

    public void reserveOrder(Order order) {
        // Publish RESERVE command for each item (or aggregated payload)
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

        publishOrderCommand(orderCommand);
        log.info("✅ Order {} placed and RELEASE command {} published", order.getId(), commandId);
    }
}
