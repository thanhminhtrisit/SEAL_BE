package com.seal.seal_backend.ranking.dto.response;

public record ScoreBreakdownResponse(
        String judgeName,
        String criterionName,
        Double criterionWeight,
        Double scoreValue,
        String judgeComment
) {}
