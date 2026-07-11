package com.seal.seal_backend.auth.dto.response;

public record RegisterResponse(Long userId, String status) {
    public static RegisterResponse pending(Long userId) {
        return new RegisterResponse(userId, "PENDING");
    }

    /** AUTO_APPROVE_ACCOUNTS=true: account passed validation and was activated immediately. */
    public static RegisterResponse active(Long userId) {
        return new RegisterResponse(userId, "ACTIVE");
    }
}
