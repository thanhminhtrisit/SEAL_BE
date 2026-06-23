package com.seal.seal_backend.scoring.dto.response;

import com.seal.seal_backend.domain.enums.EvaluationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EvaluationResponse {

    private Long id;

    private Long judgeAssignmentId;

    private Long judgeId;

    private Long submissionVersionId;

    private Long roundId;

    private EvaluationStatus status;

    private String generalComment;

    private BigDecimal totalRawScore;

    private BigDecimal totalWeightedScore;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ScoreResponse> scores;
}
