package com.seal.seal_backend.scoring.service;

import com.seal.seal_backend.shared.contract.ScoringQueryPort;
import com.seal.seal_backend.shared.contract.dto.ScoreView;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

/** OWNER: M2. Stub of ScoringQueryPort. */
@Service
public class ScoringQueryAdapter implements ScoringQueryPort {
    @Override public List<ScoreView> scoresForRound(Long roundId) { return Collections.emptyList(); }
}
