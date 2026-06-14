package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.shared.contract.SubmissionQueryPort;
import com.seal.seal_backend.shared.contract.dto.SubmissionVersionView;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** OWNER: M2. Stub of SubmissionQueryPort. */
@Service
public class SubmissionQueryAdapter implements SubmissionQueryPort {
    @Override public Optional<SubmissionVersionView> currentVersion(Long submissionId) { return Optional.empty(); }
    @Override public List<SubmissionVersionView> currentVersionsForRound(Long roundId) { return Collections.emptyList(); }
}
