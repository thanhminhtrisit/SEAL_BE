package com.seal.seal_backend.scoring.service;

import com.seal.seal_backend.domain.entity.Score;
import com.seal.seal_backend.domain.repository.ScoreRepository;
import com.seal.seal_backend.shared.contract.ScoringQueryPort;
import com.seal.seal_backend.shared.contract.dto.ScoreView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** OWNER: M2. Stub of ScoringQueryPort. */
@Service
@RequiredArgsConstructor
public class ScoringQueryAdapter implements ScoringQueryPort {

    private final ScoreRepository scoreRepository;

    @Override
    public List<ScoreView> scoresForRound(Long roundId) {
        List<Score> scores = scoreRepository.findValidScoresByRoundId(roundId);

        if (scores.isEmpty()) {
            return List.of();
        }

        // Map sang DTO để trả về cho Module khác
        return scores.stream().map(score -> new ScoreView(
                score.getEvaluation().getSubmission().getTeam().getId(), // Lấy ID đội thi
                score.getCriterion().getId(),
                score.getScoreValue().doubleValue()
        )).collect(Collectors.toList());
    }
}
