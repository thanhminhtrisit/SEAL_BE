package com.seal.seal_backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateGuestJudgeRequest(
        @Email @NotBlank String email,
        @NotBlank String fullName,
        String phone
) {}
