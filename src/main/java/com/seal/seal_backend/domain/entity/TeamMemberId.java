package com.seal.seal_backend.domain.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for TeamMember (team_id, user_id). Field names MUST match @Id fields in TeamMember. */
public class TeamMemberId implements Serializable {
    private Long team;
    private Long user;
    public TeamMemberId() {}
    public TeamMemberId(Long team, Long user) { this.team = team; this.user = user; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamMemberId that)) return false;
        return Objects.equals(team, that.team) && Objects.equals(user, that.user);
    }
    @Override public int hashCode() { return Objects.hash(team, user); }
}
