package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.seal.seal_backend.domain.enums.SubmissionStatus;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByTeamId(Long teamId);

    List<Submission> findByRoundId(Long roundId);

    Optional<Submission> findFirstByTeamIdAndRoundIdAndStatusOrderByAttemptNumberDesc(
            Long teamId, Long roundId, SubmissionStatus status);

    Optional<Submission> findFirstByTeamIdAndRoundIdOrderByAttemptNumberDesc(Long teamId, Long roundId);

    List<Submission> findByTeamIdAndRoundIdOrderByAttemptNumberDesc(Long teamId, Long roundId);

    @Query("select coalesce(max(s.attemptNumber), 0) from Submission s where s.team.id = :teamId and s.round.id = :roundId")
    Integer findMaxAttemptNumber(@Param("teamId") Long teamId, @Param("roundId") Long roundId);

    @Query("""
            select s
            from Submission s
            join fetch s.team t
            join fetch t.category c
            join fetch t.event e
            join fetch s.round r
            where r.id in :roundIds
              and s.status = :status
            order by r.id asc, t.id asc, s.attemptNumber desc
            """)
    List<Submission> findSubmittedByRoundIds(
            @Param("roundIds") Collection<Long> roundIds,
            @Param("status") SubmissionStatus status
    );

    // Vẫn giữ lại hàm cũ nếu có nơi khác trong dự án đang dùng
    boolean existsByTeamIdAndRoundId(Long teamId, Long roundId);

    /** Latest attempt whose status is one of the given set — used to read the "current" submission
     *  both while a round is open (SUBMITTED) and after it is locked (LOCKED). */
    Optional<Submission> findFirstByTeamIdAndRoundIdAndStatusInOrderByAttemptNumberDesc(
            Long teamId, Long roundId, Collection<SubmissionStatus> statuses);

    /** Bulk lifecycle transition for every submission of a round currently in a given status
     *  (SUBMITTED→LOCKED when the round is locked, LOCKED→SUBMITTED on audited unlock). */
    @Modifying
    @Query("update Submission s set s.status = :to where s.round.id = :roundId and s.status = :from")
    int bulkUpdateStatusByRound(@Param("roundId") Long roundId,
                                @Param("from") SubmissionStatus from,
                                @Param("to") SubmissionStatus to);

    // 🌟 THÊM MỚI: Lấy danh sách các team_id ĐÃ CÓ submission trong một Round (Giải quyết N+1 Query)
    @Query("SELECT s.team.id FROM Submission s WHERE s.round.id = :roundId AND s.team.id IN :teamIds")
    List<Long> findExistingSubmissionTeamIds(
            @Param("roundId") Long roundId,
            @Param("teamIds") List<Long> teamIds
    );

    /**
     * BR-SUB-01 — records a rejected LATE submission attempt in its OWN transaction so the
     * audit/history row survives the caller's rollback (the submit itself is still rejected).
     * Status LATE_REJECTED is excluded from every SUBMITTED/LOCKED read path, so it never
     * counts toward scoring, ranking, or the "current" submission — it only appears in history.
     */
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "INSERT INTO submissions (team_id, round_id, submitted_by, attempt_number, " +
            "repo_url, demo_url, slide_url, report_url, change_note, status, submitted_at, created_at) " +
            "VALUES (:teamId, :roundId, :submittedBy, :attemptNumber, :repoUrl, :demoUrl, :slideUrl, " +
            ":reportUrl, :changeNote, 'LATE_REJECTED', NOW(), NOW())", nativeQuery = true)
    void recordLateRejectedAttempt(@Param("teamId") Long teamId,
                                   @Param("roundId") Long roundId,
                                   @Param("submittedBy") Long submittedBy,
                                   @Param("attemptNumber") Integer attemptNumber,
                                   @Param("repoUrl") String repoUrl,
                                   @Param("demoUrl") String demoUrl,
                                   @Param("slideUrl") String slideUrl,
                                   @Param("reportUrl") String reportUrl,
                                   @Param("changeNote") String changeNote);
}