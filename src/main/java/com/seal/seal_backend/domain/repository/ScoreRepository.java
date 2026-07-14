package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(Long evaluationId);

    Optional<Score> findByEvaluation_IdAndCriterion_Id(Long evaluationId, Long criterionId);

    @Query("SELECT s FROM Score s JOIN s.evaluation e WHERE e.round.id = :roundId AND e.status IN ('SUBMITTED', 'LOCKED')")
    List<Score> findValidScoresByRoundId(@Param("roundId")Long roundId);

    @Query("SELECT s FROM Score s WHERE s.evaluation.id IN :evaluationIds")
    List<Score> findByEvaluationIdIn(Set<Long> evaluationIds);

}
