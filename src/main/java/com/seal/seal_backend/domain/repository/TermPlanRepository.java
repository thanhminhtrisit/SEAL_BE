package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TermPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermPlanRepository extends JpaRepository<TermPlan, Long> {

    @Query("SELECT tp FROM TermPlan tp WHERE " +
           "(:disciplineId IS NULL OR tp.discipline.id = :disciplineId) AND " +
           "(:year IS NULL OR tp.year = :year) " +
           "ORDER BY tp.year DESC, tp.term ASC")
    List<TermPlan> findByOptionalFilters(@Param("disciplineId") Long disciplineId,
                                         @Param("year") Integer year);
}
