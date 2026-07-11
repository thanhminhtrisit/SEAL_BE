package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.TeamInvitation;
import com.seal.seal_backend.domain.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    boolean existsByTeamIdAndEmail(Long teamId, String email);
    /** One row per (team,email) — DB UNIQUE constraint. Re-invites must reuse this row. */
    Optional<TeamInvitation> findByTeamIdAndEmail(Long teamId, String email);
    List<TeamInvitation> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<TeamInvitation> findByEmailAndStatus(String email, InvitationStatus status);
}
