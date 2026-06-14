package com.seal.seal_backend.common.api;

import java.time.Instant;

/**
 * Standard response envelope used by ALL modules. Owned by Lead (frozen).
 * Do not change the shape without team agreement — every controller depends on it.
 */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, Instant.now());
    }
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
