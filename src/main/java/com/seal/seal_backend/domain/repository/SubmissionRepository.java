package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTeamId(Long teamId);

    List<Submission> findByRoundId(Long roundId);

    Optional<Submission> findByTeamIdAndRoundId(
            Long teamId,
            Long roundId
    );
}
