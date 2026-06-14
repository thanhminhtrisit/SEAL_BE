package com.seal.seal_backend.domain.enums;

/** Mirrors the CHECK constraint values in the SQL schema. */
public enum EventStatus {
    DRAFT,PENDING_APPROVAL,REJECTED,APPROVED,OPEN,IN_PROGRESS,COMPLETED,ARCHIVED
}
