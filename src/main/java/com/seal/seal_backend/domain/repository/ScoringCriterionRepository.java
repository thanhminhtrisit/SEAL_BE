package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.ScoringCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.List;

@Repository
public interface ScoringCriterionRepository extends JpaRepository<ScoringCriterion, Long> {
<<<<<<< HEAD
    List<ScoringCriterion> findByCriteriaSetIdOrderByDisplayOrder(Long criteriaSetId);
    List<ScoringCriterion> findByCriteriaSetIdAndIsActiveTrueOrderByDisplayOrder(Long criteriaSetId);
=======

    List<ScoringCriterion> findByCriteriaSet_Round_IdAndIsActiveTrueOrderByDisplayOrderAsc(Long roundId);
>>>>>>> 448847f729bb11db6a7751276f7135516132958b
}
