package com.seal.seal_backend.domain.enums;

/** Mirrors the CHECK constraint values in the SQL schema. */
public enum RoundStatus {
    DRAFT,OPEN_FOR_SUBMISSION,SUBMISSION_CLOSED,SCORING_OPEN,SCORING_LOCKED,COMPLETED
}
