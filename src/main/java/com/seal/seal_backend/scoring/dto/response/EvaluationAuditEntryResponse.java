package com.seal.seal_backend.scoring.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EvaluationAuditEntryResponse {

    private Long id;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String oldValue;
    private String newValue;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private LocalDateTime createdAt;
}
