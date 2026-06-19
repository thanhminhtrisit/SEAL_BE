package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.SubmissionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionVersionRepository extends JpaRepository<SubmissionVersion, Long> {
    List<SubmissionVersion> findBySubmissionIdOrderByVersionNumberDesc(
            Long submissionId
    );

    Optional<SubmissionVersion> findTopBySubmissionIdOrderByVersionNumberDesc(
            Long submissionId
    );
}
