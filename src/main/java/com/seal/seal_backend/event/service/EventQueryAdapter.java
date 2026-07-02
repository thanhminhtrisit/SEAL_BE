package com.seal.seal_backend.event.service;

import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.repository.CriteriaSetRepository;
import com.seal.seal_backend.domain.repository.RoundRepository;
import com.seal.seal_backend.domain.repository.ScoringCriterionRepository;
import com.seal.seal_backend.shared.contract.EventQueryPort;
import com.seal.seal_backend.shared.contract.dto.CriterionView;
import com.seal.seal_backend.shared.contract.dto.RoundView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** OWNER: M1. Real implementation of EventQueryPort consumed by M2/M3. */
@Service
@RequiredArgsConstructor
public class EventQueryAdapter implements EventQueryPort {

    private final RoundRepository roundRepository;
    private final CriteriaSetRepository criteriaSetRepository;
    private final ScoringCriterionRepository scoringCriterionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RoundView> findRound(Long roundId) {
        return roundRepository.findById(roundId).map(r -> new RoundView(
                r.getId(),
                r.getEvent().getId(),
                r.getName(),
                r.getOrderNumber(),
                r.getStatus(),
                r.getSubmissionDeadline(),
                r.getPromotionTopN(),
                r.getIsFinalRound(),
                r.getRequiresRepo(),
                r.getRequiresDemo(),
                r.getRequiresSlide(),
                r.getRequiresReport()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoundOpenForSubmission(Long roundId) {
        return roundRepository.findById(roundId)
                .map(r -> r.getStatus() == RoundStatus.OPEN_FOR_SUBMISSION)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoundScoringOpen(Long roundId) {
        return roundRepository.findById(roundId)
                .map(r -> r.getStatus() == RoundStatus.SCORING_OPEN)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriterionView> criteriaForRound(Long roundId) {
        return criteriaSetRepository.findFirstByRoundId(roundId)
                .map(cs -> scoringCriterionRepository
                        .findByCriteriaSetIdAndIsActiveTrueOrderByDisplayOrder(cs.getId())
                        .stream()
                        .map(c -> new CriterionView(
                                c.getId(), c.getCriteriaSet().getId(), c.getName(),
                                c.getMaxScore(), c.getWeight(), c.getDisplayOrder()))
                        .toList())
                .orElse(Collections.emptyList());
    }
}
