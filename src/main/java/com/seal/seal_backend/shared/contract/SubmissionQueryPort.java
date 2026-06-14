package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.SubmissionVersionView;
import java.util.List;
import java.util.Optional;

/** IMPLEMENTED BY: submission module (M2). CONSUMED BY: scoring (M2), ranking (M3). */
public interface SubmissionQueryPort {
    Optional<SubmissionVersionView> currentVersion(Long submissionId);
    /** Current submission versions of all non-disqualified teams in a round. */
    List<SubmissionVersionView> currentVersionsForRound(Long roundId);
}
