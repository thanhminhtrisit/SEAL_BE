package com.seal.seal_backend.auth.dto.response;

import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.AccountType;
import com.seal.seal_backend.domain.enums.UserStatus;

import java.time.LocalDateTime;

public record MeResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String roleCode,
        AccountType accountType,
        UserStatus status,
        String studentId,
        String university,
        Boolean isFptStudent,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static MeResponse from(User u) {
        return new MeResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getPrimaryRole() != null ? u.getPrimaryRole().getCode() : null,
                u.getAccountType(),
                u.getStatus(),
                u.getStudentId(),
                u.getUniversity(),
                u.getIsFptStudent(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}
