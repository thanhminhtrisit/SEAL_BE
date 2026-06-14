package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.CriterionView;
import com.seal.seal_backend.shared.contract.dto.RoundView;
import java.util.List;
import java.util.Optional;

/**
 * Cross-module read API for Events/Rounds/Criteria.
 * IMPLEMENTED BY: event module (M1). CONSUMED BY: submission (M2), scoring (M2), ranking (M3).
 * Depend on THIS interface, not on event internals, so you can develop in parallel.
 */
public interface EventQueryPort {
    Optional<RoundView> findRound(Long roundId);
    boolean isRoundOpenForSubmission(Long roundId);
    boolean isRoundScoringOpen(Long roundId);
    /** Active criteria for the round's criteria set (weights sum to 100 — BR-EVT-03). */
    List<CriterionView> criteriaForRound(Long roundId);
}
