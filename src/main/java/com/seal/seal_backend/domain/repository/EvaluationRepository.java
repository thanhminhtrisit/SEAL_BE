package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByJudge_IdAndSubmissionVersion_IdAndRound_Id(
            Long judgeId,
            Long submissionVersionId,
            Long roundId
    );
}
