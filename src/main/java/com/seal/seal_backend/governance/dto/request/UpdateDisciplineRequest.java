package com.seal.seal_backend.governance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** FR-GOV-01 — partial update: rename, edit description, or activate/deactivate. All fields optional. */
public record UpdateDisciplineRequest(String name, String description,
                                      @JsonProperty("active") Boolean isActive) {}
