package com.shopfast.productservice.util;

import com.shopfast.productservice.exception.InvalidUuidException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for UUID validation
 */
public final class UuidValidator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private UuidValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates if the given string is a valid UUID
     * 
     * @param uuid the string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Validates the UUID string and throws InvalidUuidException if invalid
     * 
     * @param uuid the string to validate
     * @param fieldName the name of the field being validated (for error message)
     * @return the validated UUID string
     * @throws InvalidUuidException if the UUID is invalid
     */
    public static String validateOrThrow(String uuid, String fieldName) {
        if (!isValid(uuid)) {
            throw new InvalidUuidException(fieldName, uuid);
        }
        return uuid;
    }

    /**
     * Parses the string to UUID and throws InvalidUuidException if invalid
     * 
     * @param uuid the string to parse
     * @param fieldName the name of the field being validated (for error message)
     * @return the parsed UUID
     * @throws InvalidUuidException if the UUID is invalid
     */
    public static UUID parseOrThrow(String uuid, String fieldName) {
        validateOrThrow(uuid, fieldName);
        return UUID.fromString(uuid);
    }
}
