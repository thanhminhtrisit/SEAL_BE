package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByEventIdOrderByCreatedAtAsc(Long eventId);
    boolean existsByEventIdAndName(Long eventId, String name);
    boolean existsByEventIdAndCategoryIdAndIdNot(Long eventId, Long categoryId, Long excludeTeamId);

    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm " +
           "WHERE tm.user.id = :userId AND tm.team.event.id = :eventId " +
           "AND tm.status = com.seal.seal_backend.domain.enums.TeamMemberStatus.ACTIVE")
    boolean existsActiveMemberByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);
}
