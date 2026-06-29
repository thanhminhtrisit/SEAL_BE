package com.seal.seal_backend.ranking.dto.response;

public record DisqualifiedTeamResponse(
        Long id,
        String name,
        String disqualifiedReason
) {}
