package com.shopfast.cartservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.cartservice.client.ProductClient;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.ProductInternalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
public class CartService {

    private final RedisTemplate<String, String> redisTemplate;

    private final ProductClient productClient;

    private final ObjectMapper objectMapper;

    public CartService(RedisTemplate<String, String> redisTemplate, ProductClient productClient, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
        this.objectMapper = objectMapper;
    }

    private String cartKey(String userId) {
        return "cart:" + userId;
    }

    // Add/Update
    public void addItem(String userId, String productId, Integer quantity) throws JsonProcessingException {
        ProductInternalResponse p = productClient.getProduct(productId);
        if (p == null || !p.getActive()) {
            throw new NoSuchElementException("Product not available");
        }

        CartItemDto itemDto = CartItemDto.builder()
                .productId(UUID.fromString(productId))
                .quantity(quantity)
                .price(p.getPrice())
                .title(p.getTitle())
                .build();

        String json = objectMapper.writeValueAsString(itemDto);
        redisTemplate.opsForHash().put(cartKey(userId), productId, json);
    }

    public List<CartItemDto> getCartItems(String userId) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(cartKey(userId));

        return map.values().stream()
                .map(v -> {
                    try {
                        return objectMapper.readValue(v.toString(), CartItemDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
    }

    // Remove Item
    public void removeItem(String userId, String productId) throws JsonProcessingException {
        redisTemplate.opsForHash().delete(cartKey(userId), productId);
    }

    // Clear Cart
    public void clearCart(String userId) {
        redisTemplate.opsForHash().delete(cartKey(userId));
    }

}
