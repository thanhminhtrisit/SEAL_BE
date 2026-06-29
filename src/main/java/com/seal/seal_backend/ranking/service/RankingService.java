package com.seal.seal_backend.ranking.service;

import com.seal.seal_backend.ranking.dto.response.CategoryResponse;
import com.seal.seal_backend.ranking.dto.response.DisqualifiedTeamResponse;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.dto.response.ScoreBreakdownResponse;
import com.seal.seal_backend.ranking.service.impl.RankingServiceImpl;

import java.util.List;

public interface RankingService {
    List<RankingResponse> computeRankingForRound(Long roundId, Long categoryId, Long userId);

    List<RankingResponse> getRankingsByRound(Long roundId);

    List<CategoryResponse> getCategoriesByEvent(Long eventId);

    void disqualifyTeam(Long teamId, String reason, Long userId);

    List<DisqualifiedTeamResponse> getDisqualifiedTeams(Long eventId);

    List<ScoreBreakdownResponse> getScoreBreakdown(Long teamId, Long roundId);

    void promoteTeamsToNextRound(Long currentRoundId, List<Long> teamIds);
}
