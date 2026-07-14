package com.seal.seal_backend.governance.service;

import com.seal.seal_backend.governance.dto.request.CreateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.CreateTermPlanRequest;
import com.seal.seal_backend.governance.dto.request.UpdateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.UpdateTermPlanRequest;
import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;

/** FR-GOV-01 / FR-GOV-02 — Super-Coordinator write operations on disciplines and term quotas. */
public interface GovernanceCommandService {
    DisciplineResponse createDiscipline(CreateDisciplineRequest req, Long actorId);
    DisciplineResponse updateDiscipline(Long id, UpdateDisciplineRequest req, Long actorId);
    TermPlanResponse createTermPlan(CreateTermPlanRequest req, Long actorId);
    TermPlanResponse updateTermPlan(Long id, UpdateTermPlanRequest req, Long actorId);
}
