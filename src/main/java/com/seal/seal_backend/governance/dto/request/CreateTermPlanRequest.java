package com.seal.seal_backend.governance.dto.request;

/** FR-GOV-02 — set a term quota (max events for a term/year/discipline). */
public record CreateTermPlanRequest(String term, Integer year, Long disciplineId, Integer maxEvents) {}
