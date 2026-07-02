package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Team;
import com.seal.seal_backend.domain.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByEventIdOrderByCreatedAtAsc(Long eventId);
    List<Team> findByEventIdAndStatusOrderByCreatedAtAsc(Long eventId, TeamStatus status);
    boolean existsByEventIdAndName(Long eventId, String name);
    boolean existsByEventIdAndCategoryIdAndIdNot(Long eventId, Long categoryId, Long excludeTeamId);
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm " +
           "WHERE tm.user.id = :userId AND tm.team.event.id = :eventId " +
           "AND tm.status = com.seal.seal_backend.domain.enums.TeamMemberStatus.ACTIVE")
    boolean existsActiveMemberByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("SELECT COUNT(t) FROM Team t WHERE t.event.id = :eventId " +
           "AND t.status IN (com.seal.seal_backend.domain.enums.TeamStatus.REGISTERED, " +
           "com.seal.seal_backend.domain.enums.TeamStatus.APPROVED, " +
           "com.seal.seal_backend.domain.enums.TeamStatus.ACTIVE)")
    long countActiveTeamsByEventId(@Param("eventId") Long eventId);
}
