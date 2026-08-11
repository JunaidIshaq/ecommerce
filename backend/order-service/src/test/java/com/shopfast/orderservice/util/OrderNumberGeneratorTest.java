package com.shopfast.orderservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberGeneratorTest {

    private final OrderNumberGenerator generator = new OrderNumberGenerator();

    @Test
    void nextReturnsWellFormedOrderNumber() {
        String number = generator.next();
        assertThat(number).matches("^ORD-\\d{8}-[0-9A-Z]{12}$");
    }

    @Test
    void generatedNumbersAreUniqueAcrossManyCalls() {
        var set = java.util.concurrent.ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 5000; i++) {
            set.add(generator.next());
        }
        assertThat(set).hasSize(5000);
    }
}
