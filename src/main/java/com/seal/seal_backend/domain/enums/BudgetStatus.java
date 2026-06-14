package com.seal.seal_backend.domain.enums;

/** Mirrors the CHECK constraint values in the SQL schema. */
public enum BudgetStatus {
    DRAFT,PENDING_APPROVAL,APPROVED,REJECTED,REQUIRES_REAPPROVAL
}
