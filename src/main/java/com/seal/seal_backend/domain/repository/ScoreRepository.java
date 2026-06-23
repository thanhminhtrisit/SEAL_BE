package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findByEvaluation_IdOrderByCriterion_DisplayOrderAsc(Long evaluationId);

    Optional<Score> findByEvaluation_IdAndCriterion_Id(Long evaluationId, Long criterionId);
}
