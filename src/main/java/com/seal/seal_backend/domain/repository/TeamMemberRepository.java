package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamMember;
import com.seal.seal_backend.domain.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
    List<TeamMember> findByTeamId(Long teamId);

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId " +
           "AND tm.status = com.seal.seal_backend.domain.enums.TeamMemberStatus.ACTIVE")
    long countActiveByTeamId(@Param("teamId") Long teamId);
}
