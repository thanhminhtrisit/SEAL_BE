package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTeamId(Long teamId);

    List<Submission> findByRoundId(Long roundId);

    Optional<Submission> findByTeamIdAndRoundId(
            Long teamId,
            Long roundId
    );

    @Modifying
    @Query(value = "INSERT INTO submissions (team_id, round_id, status) VALUES (:teamId, :roundId, 'SUBMITTED')", nativeQuery = true)
    void createPlaceholderSubmission(@Param("teamId") Long teamId, @Param("roundId") Long roundId);

    // Kiểm tra xem đội đã có bài nộp ở vòng đó chưa
    boolean existsByTeamIdAndRoundId(Long teamId, Long roundId);


}
