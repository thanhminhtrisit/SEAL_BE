package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.InvitationStatus;
@Entity @Table(name = "team_invitations")
@Getter @Setter @NoArgsConstructor
public class TeamInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @Column(nullable = false) private String email;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invited_user_id")
    private User invitedUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private InvitationStatus status = InvitationStatus.PENDING;
    @Column(length = 128) private String token;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "accepted_at") private LocalDateTime acceptedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
