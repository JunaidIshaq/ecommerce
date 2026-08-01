package com.shopfast.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for AES-256-GCM password encryption/decryption.
 *
 * <p><strong>Encryption Scheme for Frontend:</strong></p>
 * <ul>
 *   <li><strong>Algorithm:</strong> AES-256-GCM (Galois/Counter Mode)</li>
 *   <li><strong>Key Size:</strong> 256 bits (32 bytes)</li>
 *   <li><strong>IV Size:</strong> 12 bytes (96 bits) — randomly generated per encryption</li>
 *   <li><strong>Authentication Tag:</strong> 128 bits (built into GCM)</li>
 *   <li><strong>Encoding:</strong> Base64</li>
 *   <li><strong>Transmission Format:</strong> Base64(IV + ciphertext + authTag)</li>
 * </ul>
 *
 * <p><strong>Frontend Implementation Notes:</strong></p>
 * <ol>
 *   <li>Obtain the 256-bit encryption key from the backend configuration (shared secret).</li>
 *   <li>Generate a random 12-byte IV for each encryption operation.</li>
 *   <li>Encrypt the plaintext password using AES-256-GCM with the key and IV.</li>
 *   <li>Concatenate IV + ciphertext + authTag into a single byte array.</li>
 *   <li>Base64-encode the concatenated bytes and send as the {@code password} field.</li>
 * </ol>
 *
 * <p><strong>Security Notes:</strong></p>
 * <ul>
 *   <li>Always use HTTPS/TLS in addition to this encryption.</li>
 *   <li>Never hardcode the encryption key in frontend source code.</li>
 *   <li>Rotate the encryption key periodically.</li>
 *   <li>GCM provides both confidentiality and integrity verification.</li>
 * </ul>
 */
public class PasswordEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes (96 bits recommended for GCM)
    private static final int KEY_SIZE = 256; // bits

    private PasswordEncryptionUtil() {
        // Utility class
    }

    /**
     * Encrypts a plaintext password using AES-256-GCM.
     *
     * @param plainText the plaintext password
     * @param secretKey the 256-bit AES secret key
     * @return Base64-encoded string containing IV + ciphertext + authTag
     */
    public static String encrypt(String plainText, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV + ciphertext (auth tag is appended by GCM automatically)
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt password", e);
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted password.
     *
     * @param encryptedText Base64-encoded string containing IV + ciphertext + authTag
     * @param secretKey     the 256-bit AES secret key
     * @return the decrypted plaintext password
     */
    public static String decrypt(String encryptedText, SecretKey secretKey) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            if (combined.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted password format");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decrypt password", e);
        }
    }

    /**
     * Generates a new AES-256 secret key.
     *
     * @return a new 256-bit AES secret key
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE);
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AES key", e);
        }
    }

    /**
     * Creates a SecretKey from a raw 32-byte (256-bit) key string.
     *
     * @param keyBytes the raw key bytes (must be 32 bytes for AES-256)
     * @return the SecretKey
     */
    public static SecretKey fromKeyBytes(byte[] keyBytes) {
        if (keyBytes.length != KEY_SIZE / 8) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes");
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Creates a SecretKey from a Base64-encoded key string.
     *
     * @param base64Key the Base64-encoded 256-bit key
     * @return the SecretKey
     */
    public static SecretKey fromBase64Key(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return fromKeyBytes(keyBytes);
    }
}
