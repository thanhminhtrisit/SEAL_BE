package com.seal.seal_backend.auth.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        String phone,
        boolean fptStudent,
        String studentId,
        String university) {}
