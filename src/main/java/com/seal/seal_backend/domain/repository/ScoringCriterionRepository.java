package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.ScoringCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScoringCriterionRepository extends JpaRepository<ScoringCriterion, Long> {
    List<ScoringCriterion> findByCriteriaSetIdOrderByDisplayOrder(Long criteriaSetId);
    List<ScoringCriterion> findByCriteriaSetIdAndIsActiveTrueOrderByDisplayOrder(Long criteriaSetId);
}
