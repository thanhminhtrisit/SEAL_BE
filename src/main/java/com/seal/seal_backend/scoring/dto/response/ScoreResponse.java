package com.seal.seal_backend.scoring.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ScoreResponse {

    private Long id;

    private Long criterionId;

    private String criterionName;

    private String criterionDescription;

    private BigDecimal maxScore;

    private BigDecimal weight;

    private BigDecimal scoreValue;

    private BigDecimal weightedScore;

    private String comment;

    private LocalDateTime scoredAt;

    private LocalDateTime updatedAt;
}
