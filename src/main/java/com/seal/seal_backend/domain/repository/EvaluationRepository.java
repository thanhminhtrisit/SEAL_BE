package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByJudge_IdAndSubmission_IdAndRound_Id(
            Long judgeId,
            Long submissionId,
            Long roundId
    );

    List<Evaluation> findByJudge_IdAndRound_IdIn(Long judgeId, Collection<Long> roundIds);

    @Query("""
            select e from Evaluation e
            join fetch e.submission s
            join fetch s.team t
            join fetch t.category c
            join fetch t.event ev
            join fetch e.round r
            where e.judge.id = :judgeId
              and e.id in :evaluationIds
            """)
    List<Evaluation> findJudgeEvaluationsByIds(
            @Param("judgeId") Long judgeId,
            @Param("evaluationIds") Collection<Long> evaluationIds
    );
}
