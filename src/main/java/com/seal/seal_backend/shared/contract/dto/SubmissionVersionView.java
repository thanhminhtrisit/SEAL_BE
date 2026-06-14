package com.seal.seal_backend.shared.contract.dto;

public record SubmissionVersionView(Long id, Long submissionId, Long teamId,
                                    Long roundId, int versionNumber, String repoUrl) {}
