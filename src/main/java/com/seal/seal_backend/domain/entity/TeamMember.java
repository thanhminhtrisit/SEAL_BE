package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
@Entity @Table(name = "team_members")
@IdClass(TeamMemberId.class)
@Getter @Setter @NoArgsConstructor
public class TeamMember {
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id")
    private Team team;
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING) @Column(name = "member_role", nullable = false, length = 30)
    private TeamMemberRole memberRole = TeamMemberRole.MEMBER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TeamMemberStatus status = TeamMemberStatus.ACTIVE;
    @CreationTimestamp @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
    @Column(name = "left_at") private LocalDateTime leftAt;
}
