package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.domain.entity.Submission;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.SubmissionRepository;
import com.seal.seal_backend.shared.contract.SubmissionQueryPort;
import com.seal.seal_backend.shared.contract.dto.SubmissionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** OWNER: M2. Stub of SubmissionQueryPort. */
@Service
@RequiredArgsConstructor
public class SubmissionQueryAdapter implements SubmissionQueryPort {

    private final SubmissionRepository submissionRepository;
    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionView> latestSubmittedAttempt(Long teamId, Long roundId) {
        return submissionRepository
                .findFirstByTeamIdAndRoundIdAndStatusOrderByAttemptNumberDesc(
                        teamId, roundId, SubmissionStatus.SUBMITTED)
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionView> latestSubmittedAttemptsForRound(Long roundId) {

        List<Submission> submissions =
                submissionRepository
                        .findByRoundId(roundId);

        return submissions.stream()

                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED)
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.getTeam().getId(),
                        s -> s,
                        (left, right) -> left.getAttemptNumber() >= right.getAttemptNumber() ? left : right))
                .values().stream()
                .map(this::toView)

                .toList();
    }

    private SubmissionView toView(Submission submission) {
        return new SubmissionView(
                submission.getId(),
                submission.getTeam().getId(),
                submission.getRound().getId(),
                submission.getAttemptNumber(),
                submission.getRepoUrl()
        );
    }

}
