package com.seal.seal_backend.auth.dto.response;

import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AccountType;

public record JudgeResponse(Long id, String fullName, String email, AccountType accountType) {
    public static JudgeResponse from(User user) {
        return new JudgeResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAccountType());
    }
}
