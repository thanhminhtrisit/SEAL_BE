package com.seal.seal_backend.team.dto.request;

import jakarta.validation.constraints.NotNull;

/** BR-TEAM-05: current leader hands leadership to an ACTIVE member of the same team. */
public record TransferLeadershipRequest(@NotNull Long newLeaderId) {}
