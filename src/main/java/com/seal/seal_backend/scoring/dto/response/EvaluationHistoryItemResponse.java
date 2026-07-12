package com.seal.seal_backend.scoring.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EvaluationHistoryItemResponse {

    private String actionType;
    private String actionLabel;
    private String actorName;
    private String criterionName;
    private String oldScoreValue;
    private String newScoreValue;
    private String oldComment;
    private String newComment;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime occurredAt;
    private String description;
}
