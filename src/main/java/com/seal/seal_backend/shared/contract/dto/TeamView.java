package com.seal.seal_backend.shared.contract.dto;

import com.seal.seal_backend.domain.enums.TeamStatus;

public record TeamView(Long id, Long eventId, Long categoryId, Long leaderId,
                       String name, TeamStatus status) {}
