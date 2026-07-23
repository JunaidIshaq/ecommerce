package com.shopfast.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Generic wrapper returned by every API endpoint.
 *
 * <p>Success example:
 * <pre>
 * {
 *   "success": true,
 *   "status":  200,
 *   "message": "Order fetched successfully",
 *   "data":    { … },
 *   "timestamp": "2026-07-23T10:00:00Z"
 * }
 * </pre>
 *
 * <p>Failure example:
 * <pre>
 * {
 *   "success": false,
 *   "status":  404,
 *   "message": "Order not found",
 *   "timestamp": "2026-07-23T10:00:00Z"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericApiResponseDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** {@code true} when the request succeeded, {@code false} otherwise. */
    private boolean success;

    /** HTTP status code mirrored in the body for client convenience. */
    private int status;

    /** Human-readable result or error message. */
    private String message;

    /** Response payload; {@code null} on failure. */
    private T data;

    /** Server-side timestamp of the response. */
    @Builder.Default
    private Instant timestamp = Instant.now();

    // -----------------------------------------------------------------------
    // Static factory helpers
    // -----------------------------------------------------------------------

    /** Build a 200-OK success response with a custom message. */
    public static <T> GenericApiResponseDto<T> success(T data, String message) {
        return GenericApiResponseDto.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    /** Build a success response with a custom HTTP status code. */
    public static <T> GenericApiResponseDto<T> success(T data, String message, int httpStatus) {
        return GenericApiResponseDto.<T>builder()
                .success(true)
                .status(httpStatus)
                .message(message)
                .data(data)
                .build();
    }

    /** Build a failure response (data will be {@code null}). */
    public static <T> GenericApiResponseDto<T> error(String message, int httpStatus) {
        return GenericApiResponseDto.<T>builder()
                .success(false)
                .status(httpStatus)
                .message(message)
                .data(null)
                .build();
    }
}
