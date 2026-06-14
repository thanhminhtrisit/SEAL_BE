package com.seal.seal_backend.shared.contract.dto;

import com.seal.seal_backend.domain.enums.RoundStatus;
import java.time.LocalDateTime;

/** Read-only view of a Round shared across modules. */
public record RoundView(Long id, Long eventId, String name, int orderNumber,
                        RoundStatus status, LocalDateTime submissionDeadline,
                        Integer promotionTopN, boolean finalRound) {}
