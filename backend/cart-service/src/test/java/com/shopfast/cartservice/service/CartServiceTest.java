package com.shopfast.cartservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopfast.cartservice.client.ProductGateway;
import com.shopfast.cartservice.dto.CartItemDto;
import com.shopfast.cartservice.dto.ProductInternalResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ProductGateway productGateway;

    @Mock
    private HashOperations<String, String, String> hashOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CartService cartService;

    @BeforeEach
    void setUp() throws Exception {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        cartService = new CartService(redisTemplate, productGateway, objectMapper);
        // @Value fields are not injected in a plain unit test; set them so key helpers work.
        setField("userPrefix", "cart:");
        setField("guestPrefix", "cart:guest:");
        setField("guestTtlDays", 14L);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = CartService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(cartService, value);
    }

    private ProductInternalResponseDto product(String title, BigDecimal price, boolean active) {
        ProductInternalResponseDto p = new ProductInternalResponseDto();
        p.setTitle(title);
        p.setPrice(price);
        p.setActive(active);
        p.setImages(List.of("img.png"));
        return p;
    }

    private CartItemDto item(UUID productId, int qty, BigDecimal price, String title) {
        return CartItemDto.builder()
                .productId(productId)
                .quantity(qty)
                .price(price)
                .title(title)
                .images(List.of("img.png"))
                .build();
    }

    @Test
    void addUserStoresSerializedItemWhenProductExists() throws Exception {
        String userId = "user-1";
        String productId = "00000000-0000-0000-0000-000000000001";
        when(productGateway.getProduct(productId)).thenReturn(product("Book", new BigDecimal("10.0"), true));
        when(hashOperations.get("cart:" + userId, productId)).thenReturn(null);

        cartService.addUser(userId, productId, 2);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), contains("\"quantity\":2"));
    }

    @Test
    void addUserSumsQuantityWhenItemAlreadyInCart() throws Exception {
        String userId = "user-1";
        String productId = "00000000-0000-0000-0000-000000000001";
        UUID pid = UUID.fromString(productId);
        when(productGateway.getProduct(productId)).thenReturn(product("Book", new BigDecimal("10.0"), true));
        when(hashOperations.get("cart:" + userId, productId))
                .thenReturn(objectMapper.writeValueAsString(item(pid, 1, new BigDecimal("10.0"), "Book")));

        cartService.addUser(userId, productId, 4);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), contains("\"quantity\":5"));
    }

    @Test
    void addUserThrowsWhenProductInactive() {
        String productId = "00000000-0000-0000-0000-000000000001";
        when(productGateway.getProduct(productId)).thenReturn(product("Book", new BigDecimal("10.0"), false));

        assertThatThrownBy(() -> cartService.addUser("u", productId, 1))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Product not available");
    }

    @Test
    void getUserCartDeserializesStoredItems() throws Exception {
        String userId = "user-1";
        UUID pid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CartItemDto stored = item(pid, 3, new BigDecimal("5.0"), "Pen");
        when(hashOperations.entries("cart:" + userId))
                .thenReturn(Map.of(pid.toString(), objectMapper.writeValueAsString(stored)));

        List<CartItemDto> result = cartService.getUserCart(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(3);
        assertThat(result.get(0).getTitle()).isEqualTo("Pen");
    }

    @Test
    void removeUserItemDeletesProductHashEntry() {
        cartService.removeUserItem("user-1", "p1");
        verify(hashOperations).delete("cart:user-1", "p1");
    }

    @Test
    void clearUserCartDeletesWholeKey() {
        cartService.clearUserCart("user-1");
        verify(redisTemplate).delete("cart:user-1");
    }

    @Test
    void addGuestStoresUnderGuestPrefix() throws Exception {
        String anonId = "00000000-0000-0000-0000-000000000099";
        String productId = "00000000-0000-0000-0000-000000000001";
        when(productGateway.getProduct(productId)).thenReturn(product("Book", new BigDecimal("9.0"), true));

        cartService.addGuest(anonId, productId, 1);

        verify(hashOperations).put(eq("cart:guest:" + anonId), eq(productId), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mergeGuestIntoUserCombinesQuantitiesAndDeletesGuestCart() throws Exception {
        String anonId = "00000000-0000-0000-0000-000000000099";
        String userId = "user-1";
        String productId = "00000000-0000-0000-0000-000000000001";
        UUID pid = UUID.fromString(productId);
        when(hashOperations.entries("cart:guest:" + anonId))
                .thenReturn(Map.of(productId, objectMapper.writeValueAsString(item(pid, 2, new BigDecimal("7.0"), "Item"))));
        when(hashOperations.get("cart:" + userId, productId))
                .thenReturn(objectMapper.writeValueAsString(item(pid, 3, new BigDecimal("7.0"), "Item")));

        cartService.mergeGuestIntoUser(anonId, userId);

        verify(hashOperations).put(eq("cart:" + userId), eq(productId), contains("\"quantity\":5"));
        verify(hashOperations).delete("cart:guest:" + anonId);
    }

    @Test
    void mergeGuestIntoUserIsNoopWhenGuestCartEmpty() throws Exception {
        when(hashOperations.entries("cart:guest:anon")).thenReturn(Map.of());

        cartService.mergeGuestIntoUser("anon", "user-1");

        verify(hashOperations).entries("cart:guest:anon");
        verify(hashOperations, never()).put(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mergeGuestIntoUserCapsCombinedQuantityAt999() throws Exception {
        String productId = "00000000-0000-0000-0000-000000000001";
        UUID pid = UUID.fromString(productId);
        when(hashOperations.entries("cart:guest:anon"))
                .thenReturn(Map.of(productId, objectMapper.writeValueAsString(item(pid, 900, new BigDecimal("1.0"), "X"))));
        when(hashOperations.get("cart:user-1", productId))
                .thenReturn(objectMapper.writeValueAsString(item(pid, 900, new BigDecimal("1.0"), "X")));

        cartService.mergeGuestIntoUser("anon", "user-1");

        verify(hashOperations).put(eq("cart:user-1"), eq(productId), contains("\"quantity\":999"));
    }
}
