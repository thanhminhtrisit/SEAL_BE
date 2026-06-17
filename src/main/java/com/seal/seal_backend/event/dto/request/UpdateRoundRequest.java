package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateRoundRequest(
        @Size(max = 150) String name,
        LocalDateTime submissionDeadline,
        Integer promotionTopN,
        Boolean finalRound
) {}
