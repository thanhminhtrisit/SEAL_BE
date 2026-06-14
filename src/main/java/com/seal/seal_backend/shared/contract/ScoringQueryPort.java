package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.ScoreView;
import java.util.List;

/** IMPLEMENTED BY: scoring module (M2). CONSUMED BY: ranking (M3), RBL (later). */
public interface ScoringQueryPort {
    /** All locked/submitted scores for a round — raw, per judge per criterion. */
    List<ScoreView> scoresForRound(Long roundId);
}
