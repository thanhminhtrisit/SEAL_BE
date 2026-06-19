package com.seal.seal_backend.auth.dto.response;

public record RegisterResponse(Long userId, String status) {
    public static RegisterResponse pending(Long userId) {
        return new RegisterResponse(userId, "PENDING");
    }
}
