package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamMember;
import com.seal.seal_backend.domain.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
}
