package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamMember;
import com.seal.seal_backend.domain.entity.TeamMemberId;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
    List<TeamMember> findByTeamId(Long teamId);

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId " +
           "AND tm.status = com.seal.seal_backend.domain.enums.TeamMemberStatus.ACTIVE")
    long countActiveByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT COUNT(DISTINCT tm.user.id) FROM TeamMember tm " +
           "WHERE tm.team.event.id = :eventId " +
           "AND tm.status = com.seal.seal_backend.domain.enums.TeamMemberStatus.ACTIVE")
    long countDistinctParticipantsByEventId(@Param("eventId") Long eventId);
    List<TeamMember> findByUser_IdAndStatusOrderByJoinedAtDesc(
            Long userId,
            TeamMemberStatus status
    );

    Optional<TeamMember> findByTeamIdAndUserIdAndStatus(
            Long teamId,
            Long userId,
            TeamMemberStatus status
    );

    boolean existsByTeamIdAndUserIdAndStatus(
            Long teamId,
            Long userId,
            TeamMemberStatus status
    );

    boolean existsByTeamIdAndUserIdAndMemberRoleAndStatus(
            Long teamId,
            Long userId,
            TeamMemberRole memberRole,
            TeamMemberStatus status
    );
}
