package com.seal.seal_backend.auth.dto.response;

import com.seal.seal_backend.domain.entity.User;

public record MentorResponse(Long id, String fullName, String email) {
    public static MentorResponse from(User user) {
        return new MentorResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
