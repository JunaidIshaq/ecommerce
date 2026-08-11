package com.shopfast.productservice.util;

import com.shopfast.productservice.exception.InvalidUuidException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UuidValidatorTest {

    private static final String VALID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void isValidAcceptsCanonicalUuid() {
        assertThat(UuidValidator.isValid(VALID)).isTrue();
    }

    @Test
    void isValidRejectsNullAndBlank() {
        assertThat(UuidValidator.isValid(null)).isFalse();
        assertThat(UuidValidator.isValid("")).isFalse();
        assertThat(UuidValidator.isValid("   ")).isFalse();
    }

    @Test
    void isValidRejectsMalformed() {
        assertThat(UuidValidator.isValid("not-a-uuid")).isFalse();
        assertThat(UuidValidator.isValid("123e4567-e89b-12d3-a456")).isFalse();
    }

    @Test
    void validateOrThrowReturnsValueWhenValid() {
        assertThat(UuidValidator.validateOrThrow(VALID, "id")).isEqualTo(VALID);
    }

    @Test
    void validateOrThrowThrowsWhenInvalid() {
        assertThatThrownBy(() -> UuidValidator.validateOrThrow("bad", "productId"))
                .isInstanceOf(InvalidUuidException.class);
    }

    @Test
    void parseOrThrowParsesValidUuid() {
        assertThat(UuidValidator.parseOrThrow(VALID, "id").toString()).isEqualTo(VALID);
    }

    @Test
    void parseOrThrowThrowsWhenInvalid() {
        assertThatThrownBy(() -> UuidValidator.parseOrThrow("bad", "id"))
                .isInstanceOf(InvalidUuidException.class);
    }
}
