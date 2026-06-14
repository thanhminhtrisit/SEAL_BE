package com.seal.seal_backend.event.service;

import com.seal.seal_backend.shared.contract.EventQueryPort;
import com.seal.seal_backend.shared.contract.dto.CriterionView;
import com.seal.seal_backend.shared.contract.dto.RoundView;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** OWNER: M1. Stub of EventQueryPort — replace with real queries. */
@Service
public class EventQueryAdapter implements EventQueryPort {
    @Override public Optional<RoundView> findRound(Long roundId) { return Optional.empty(); }
    @Override public boolean isRoundOpenForSubmission(Long roundId) { return false; }
    @Override public boolean isRoundScoringOpen(Long roundId) { return false; }
    @Override public List<CriterionView> criteriaForRound(Long roundId) { return Collections.emptyList(); }
}
