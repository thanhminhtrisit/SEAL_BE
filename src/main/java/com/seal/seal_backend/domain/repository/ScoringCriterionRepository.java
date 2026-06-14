package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.ScoringCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoringCriterionRepository extends JpaRepository<ScoringCriterion, Long> {
}
