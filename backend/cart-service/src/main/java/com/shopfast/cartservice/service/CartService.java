package com.shopfast.cartservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.cartservice.client.ProductClient;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.ProductInternalResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    @Value("${cart.redis.prefix.user:cart:}")
    private String userPrefix;

    @Value("${cart.redis.prefix.guest:cart:guest:}")
    private String guestPrefix;

    @Value("${cart.redis.ttl-days:14}")
    private long guestTtlDays;

    private String keyUser(String userId) {
        return userPrefix + userId;
    }

    private String keyGuest(String anonId) {
        return guestPrefix + anonId;
    }


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
        ProductInternalResponseDto p = productClient.getProduct(productId);
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

    public void mergeCart(String anonId, Authentication auth) {
        var guestKey = "cart:guest:" + anonId;
        var userKey = "cart:" + auth.getName();
        var guest = redisTemplate.opsForHash().entries(guestKey);
        guest.forEach((pid, json) -> redisTemplate.opsForHash().put(userKey, pid, json));
        redisTemplate.delete(guestKey);
    }

    /*--------------- Common Helpers ----------------*/
    private CartItemDto parse(String json) {
        try {
            return objectMapper.readValue(json, CartItemDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Bad cart item JSON: ", e);
        }
    }

    private String toJson(CartItemDto itemDto) {
        try {
            return objectMapper.writeValueAsString(itemDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialize cart item failed : ", e);
        }
    }

    private void touchGuestTtl(String key) {
        if (guestTtlDays > 0) {
            redisTemplate.expire(key, Duration.ofDays(guestTtlDays));
        }
    }

    public void addOrUpdateUser(String userId, String productId, Integer quantity) throws JsonProcessingException {
        ProductInternalResponseDto p = productClient.getProduct(productId);
        if (p == null || Boolean.FALSE.equals(p.getActive())) {
            throw new NoSuchElementException("Product not available");
        }

        String key = keyUser(userId);
        String existing = (String) redisTemplate.opsForHash().get(key, productId);


        int newQuantity = quantity;
        if (existing != null) {
            newQuantity += Math.max(0, parse(existing).getQuantity());
        }

        CartItemDto itemDto = CartItemDto.builder()
                .productId(UUID.fromString(productId))
                .quantity(newQuantity)
                .price(p.getPrice())
                .title(p.getTitle())
                .build();
        redisTemplate.opsForHash().put(cartKey(userId), productId, toJson(itemDto));
    }

    public List<CartItemDto> getUserCart(String userId) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(keyUser(userId));
        return map.values().stream().map(o -> parse(o.toString())).toList();
    }

    public void removeUserItem(String userId, String productId) {
        redisTemplate.opsForHash().delete(keyUser(userId), productId);
    }

    public void clearUserCart(String userId) {
        redisTemplate.delete(keyUser(userId));
    }

    /*--------------- GUEST CART ----------------*/

    public void addOrUpdateGuest(String anonId, String productId, Integer quantity) throws JsonProcessingException {
        ProductInternalResponseDto p = productClient.getProduct(productId);
        if (p == null || Boolean.FALSE.equals(p.getActive())) {
            throw new NoSuchElementException("Product not available");
        }

        String key = keyGuest(anonId);
        String existing = (String) redisTemplate.opsForHash().get(key, productId);

        int  newQuantity = quantity;
        if(existing != null) {
            newQuantity += Math.max(0, parse(existing).getQuantity());
        }

        CartItemDto itemDto = CartItemDto.builder()
                .productId(UUID.fromString(productId))
                .quantity(newQuantity)
                .price(p.getPrice())
                .title(p.getTitle())
                .build();

        redisTemplate.opsForHash().put(key, productId, toJson(itemDto));
        touchGuestTtl(key);
    }

    public List<CartItemDto> getGuestCart(String anonId) {
        String key = keyGuest(anonId);
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        touchGuestTtl(key);
        return map.values().stream().map(o -> parse(o.toString())).toList();
    }

    public void removeGuestItem(String anonId, String productId) {
        String key = keyGuest(anonId);
        redisTemplate.opsForHash().delete(key, productId);
        touchGuestTtl(key);
    }

    public void clearGuestCart(String anonId) {
        redisTemplate.delete(keyGuest(anonId));
    }
    
    /*------------- MERGE (guest -> user) --------------*/
    
    public void mergeGuestIntoUser(String anonId, String userId) throws JsonProcessingException {
        String gKey = keyGuest(anonId);
        String uKey = keyUser(userId);
        
        Map<Object, Object> guest = redisTemplate.opsForHash().entries(gKey);
        if(guest == null || guest.isEmpty() ) {
            return;
        }
        
        // Sum quantities for duplicate products; keep user's current price snapshot
        for(Map.Entry<Object, Object> entry : guest.entrySet()) {
            String productId = entry.getKey().toString();
            CartItemDto gItem = parse(entry.getValue().toString());

            String uExisting = (String) redisTemplate.opsForHash().get(uKey, productId);
            int quantity = gItem.getQuantity();

            CartItemDto merged;

            if(uExisting != null) {
                CartItemDto uItem = parse(uExisting);
                int totalQuantity = Math.min(999, Math.max(1, uItem.getQuantity() + quantity)); // cap at 999
                // Keep user's snapshot price/title (or choose newest guest price by business rule)
                merged = CartItemDto.builder()
                        .productId(UUID.fromString(productId))
                        .quantity(totalQuantity)
                        .price(uItem.getPrice())
                        .title(uItem.getTitle())
                        .build();
            } else {
                merged = gItem; // take guest snapshot
            }
            redisTemplate.opsForHash().put(uKey, productId, toJson(merged));

        }
        // Remove guest cart after merge
        redisTemplate.delete(gKey);
    }
}
