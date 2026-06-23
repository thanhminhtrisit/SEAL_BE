package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.JudgeAssignment;

import java.time.LocalDateTime;

public record JudgeAssignmentResponse(
        Long id,
        Long judgeId,
        String judgeName,
        String judgeEmail,
        Long roundId,
        Long categoryId,
        String categoryName,
        LocalDateTime assignedAt,
        String status
) {
    public static JudgeAssignmentResponse from(JudgeAssignment ja) {
        return new JudgeAssignmentResponse(
                ja.getId(),
                ja.getJudge().getId(),
                ja.getJudge().getFullName(),
                ja.getJudge().getEmail(),
                ja.getRound().getId(),
                ja.getCategory() != null ? ja.getCategory().getId() : null,
                ja.getCategory() != null ? ja.getCategory().getName() : null,
                ja.getAssignedAt(),
                ja.getStatus().name()
        );
    }
}
