package com.seal.seal_backend.governance.dto.request;

/** FR-GOV-02 — update the max-events quota of an existing term plan. */
public record UpdateTermPlanRequest(Integer maxEvents) {}
