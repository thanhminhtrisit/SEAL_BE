package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // 🌟 THÊM MỚI: Lấy danh sách các team_id ĐÃ CÓ submission trong một Round (Giải quyết N+1 Query)
    @Query("SELECT s.team.id FROM Submission s WHERE s.round.id = :roundId AND s.team.id IN :teamIds")
    List<Long> findExistingSubmissionTeamIds(
            @Param("roundId") Long roundId,
            @Param("teamIds") List<Long> teamIds
    );

    // 🌟 SỬA ĐỔI: Chuyển trạng thái mặc định của Placeholder thành 'DRAFT' (Thay vì 'SUBMITTED')
    // và thêm 'created_at' để tránh lỗi timestamp.
    @Modifying
    @Query(value = "INSERT INTO submissions (team_id, round_id, status, created_at) " +
            "VALUES (:teamId, :roundId, 'DRAFT', NOW())", nativeQuery = true)
    void createPlaceholderSubmission(
            @Param("teamId") Long teamId,
            @Param("roundId") Long roundId
    );

    // Kiểm tra xem đội đã có bài nộp ở vòng đó chưa
}