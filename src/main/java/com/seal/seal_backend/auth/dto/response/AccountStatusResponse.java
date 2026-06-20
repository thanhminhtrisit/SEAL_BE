package com.seal.seal_backend.auth.dto.response;

import com.seal.seal_backend.domain.enums.UserStatus;

public record AccountStatusResponse(Long userId, UserStatus status) {}
