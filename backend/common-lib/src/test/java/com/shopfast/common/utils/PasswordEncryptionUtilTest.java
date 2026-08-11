package com.shopfast.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncryptionUtilTest {

    @Test
    void encryptThenDecryptRoundTrips() {
        SecretKey key = PasswordEncryptionUtil.generateKey();
        String plain = "SuperSecret123!";

        String encrypted = PasswordEncryptionUtil.encrypt(plain, key);
        String decrypted = PasswordEncryptionUtil.decrypt(encrypted, key);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void eachEncryptionProducesDifferentCiphertext() {
        SecretKey key = PasswordEncryptionUtil.generateKey();
        String a = PasswordEncryptionUtil.encrypt("password", key);
        String b = PasswordEncryptionUtil.encrypt("password", key);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void decryptedValueMatchesOriginalForUnicode() {
        SecretKey key = PasswordEncryptionUtil.generateKey();
        String plain = "pässwörd-ß-🔒";

        String decrypted = PasswordEncryptionUtil.decrypt(PasswordEncryptionUtil.encrypt(plain, key), key);

        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void decryptRejectsMalformedInput() {
        SecretKey key = PasswordEncryptionUtil.generateKey();

        assertThatThrownBy(() -> PasswordEncryptionUtil.decrypt("short", key))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromKeyBytesRequires32Bytes() {
        byte[] good = new byte[32];
        byte[] bad = new byte[16];

        assertThat(PasswordEncryptionUtil.fromKeyBytes(good).getEncoded()).hasSize(32);
        assertThatThrownBy(() -> PasswordEncryptionUtil.fromKeyBytes(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
