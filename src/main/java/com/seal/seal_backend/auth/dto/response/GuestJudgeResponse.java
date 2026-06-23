package com.seal.seal_backend.auth.dto.response;

public record GuestJudgeResponse(
        Long userId,
        String email,
        String fullName,
        String temporaryPassword
) {}
