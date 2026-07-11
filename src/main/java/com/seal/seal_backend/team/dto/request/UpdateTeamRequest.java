package com.seal.seal_backend.team.dto.request;

import jakarta.validation.constraints.Size;

/** FR-TEAM-06: leader edits name/description before approval. Null field = keep current value. */
public record UpdateTeamRequest(
        @Size(max = 150) String name,
        @Size(max = 2000) String description
) {}
