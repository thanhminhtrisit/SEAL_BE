package com.seal.seal_backend.award.dto.response;

public record ParticipantResultResponse(
        Long teamId,
        String teamName,
        String categoryName,
        Integer rankPosition,
        Double totalScore,
        String awardType,
        String awardDescription
) {}