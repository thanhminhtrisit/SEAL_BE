package com.seal.seal_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectAccountRequest(@NotBlank String reason) {}
