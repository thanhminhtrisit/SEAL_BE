package com.seal.seal_backend.shared.contract.dto;

public record SubmissionView(Long id, Long teamId, Long roundId,
                             int attemptNumber, String repoUrl) {}
