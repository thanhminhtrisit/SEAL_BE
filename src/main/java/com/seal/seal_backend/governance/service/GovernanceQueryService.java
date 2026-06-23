package com.seal.seal_backend.governance.service;

import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;

import java.util.List;

public interface GovernanceQueryService {
    List<DisciplineResponse> listActiveDisciplines();
    List<TermPlanResponse> listTermPlans(Long disciplineId, Integer year);
}
