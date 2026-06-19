package com.seal.seal_backend.auth.dto.response;

import com.seal.seal_backend.domain.entity.User;
import java.time.LocalDateTime;

public record PendingAccountResponse(
        Long id,
        String fullName,
        String email,
        String studentId,
        String university,
        Boolean fptStudent,
        LocalDateTime createdAt
) {
    public static PendingAccountResponse from(User u) {
        return new PendingAccountResponse(
                u.getId(), u.getFullName(), u.getEmail(),
                u.getStudentId(), u.getUniversity(), u.getIsFptStudent(), u.getCreatedAt());
    }
}
