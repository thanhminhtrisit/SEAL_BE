package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamMember;
import com.seal.seal_backend.domain.entity.TeamMemberId;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
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
