package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.SubmissionView;
import java.util.List;
import java.util.Optional;

/** IMPLEMENTED BY: submission module (M2). CONSUMED BY: scoring (M2), ranking (M3). */
public interface SubmissionQueryPort {
    Optional<SubmissionView> latestSubmittedAttempt(Long teamId, Long roundId);
    /** Latest submitted attempt of every non-disqualified team in a round. */
    List<SubmissionView> latestSubmittedAttemptsForRound(Long roundId);
}
