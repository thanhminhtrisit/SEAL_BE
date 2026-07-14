package com.seal.seal_backend.governance.dto.request;

/** FR-GOV-01 — create a new discipline. */
public record CreateDisciplineRequest(String code, String name, String description) {}
