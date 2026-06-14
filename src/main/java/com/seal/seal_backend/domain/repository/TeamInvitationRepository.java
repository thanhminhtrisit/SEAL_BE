package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
}
