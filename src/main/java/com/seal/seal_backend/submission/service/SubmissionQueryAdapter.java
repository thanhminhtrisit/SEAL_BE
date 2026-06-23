package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.domain.entity.Submission;
import com.seal.seal_backend.domain.entity.SubmissionVersion;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.SubmissionRepository;
import com.seal.seal_backend.domain.repository.SubmissionVersionRepository;
import com.seal.seal_backend.shared.contract.SubmissionQueryPort;
import com.seal.seal_backend.shared.contract.dto.SubmissionVersionView;
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
    private final SubmissionVersionRepository submissionVersionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionVersionView>
    currentVersion(Long submissionId) {

        Submission submission =
                submissionRepository.findById(submissionId)
                        .orElse(null);

        if (submission == null ||
                submission.getCurrentVersionId() == null) {

            return Optional.empty();
        }

        return submissionVersionRepository
                .findById(
                        submission.getCurrentVersionId()
                )
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionVersionView>
    currentVersionsForRound(Long roundId) {

        List<Submission> submissions =
                submissionRepository
                        .findByRoundId(roundId);

        return submissions.stream()

                .filter(s ->
                        s.getStatus()
                                != SubmissionStatus.DISQUALIFIED)

                .filter(s ->
                        s.getCurrentVersionId() != null)

                .map(Submission::getCurrentVersionId)

                .map(submissionVersionRepository::findById)

                .filter(Optional::isPresent)

                .map(Optional::get)

                .map(this::toView)

                .toList();
    }

    private SubmissionVersionView toView(
            SubmissionVersion version
    ) {
        return new SubmissionVersionView(
                version.getId(),
                version.getSubmission().getId(),
                version.getSubmission().getTeam().getId(),
                version.getSubmission().getRound().getId(),
                version.getVersionNumber(),
                version.getRepoUrl()
        );
    }

}
