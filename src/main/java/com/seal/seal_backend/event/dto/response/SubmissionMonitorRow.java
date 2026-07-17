package com.seal.seal_backend.event.dto.response;

import java.time.LocalDateTime;

/**
 * One row of the coordinator "Submission Monitoring" screen: a single team of an event and its
 * latest submission state for a given round. {@code status} is the real {@code SubmissionStatus}
 * name, or {@code "NOT_SUBMITTED"} when the team has no submission for that round (in which case the
 * attempt/urls/timestamp are null).
 */
public record SubmissionMonitorRow(
        Long teamId,
        String teamName,
        Long categoryId,
        String categoryName,
        String status,
        Integer latestAttemptNumber,
        LocalDateTime submittedAt,
        String repoUrl,
        String demoUrl,
        String slideUrl,
        String reportUrl
) {
    /** Sentinel status used when a team has not submitted anything for the round. */
    public static final String NOT_SUBMITTED = "NOT_SUBMITTED";
}
