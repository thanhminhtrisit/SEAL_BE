package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.JudgeAssignment;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Long> {

    boolean existsByJudgeIdAndRoundIdAndStatus(Long judgeId, Long roundId, AssignmentStatus status);

    List<JudgeAssignment> findByRoundIdAndStatus(Long roundId, AssignmentStatus status);

    @Query("SELECT ja.judge.id FROM JudgeAssignment ja WHERE ja.round.id = :roundId " +
           "AND ja.status = com.seal.seal_backend.domain.enums.AssignmentStatus.ACTIVE")
    List<Long> findJudgeIdsByRoundId(@Param("roundId") Long roundId);

    boolean existsByRoundId(Long roundId);
}
