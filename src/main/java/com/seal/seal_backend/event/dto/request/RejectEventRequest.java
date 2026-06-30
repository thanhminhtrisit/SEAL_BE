package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectEventRequest(@NotBlank String reason) {}
