package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.NotBlank;

/** BR-SCR-05: unlocking a locked scoring round is exceptional and must carry an audited reason. */
public record UnlockRoundRequest(@NotBlank String reason) {}
