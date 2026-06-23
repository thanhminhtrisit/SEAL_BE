package com.seal.seal_backend.governance.service.impl;

import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.repository.DisciplineRepository;
import com.seal.seal_backend.domain.repository.EventRepository;
import com.seal.seal_backend.domain.repository.TermPlanRepository;
import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;
import com.seal.seal_backend.governance.service.GovernanceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GovernanceQueryServiceImpl implements GovernanceQueryService {

    private final DisciplineRepository disciplineRepository;
    private final TermPlanRepository termPlanRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DisciplineResponse> listActiveDisciplines() {
        return disciplineRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(DisciplineResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermPlanResponse> listTermPlans(Long disciplineId, Integer year) {
        return termPlanRepository.findByOptionalFilters(disciplineId, year)
                .stream()
                .map(tp -> {
                    long used = eventRepository.countByTermPlanIdAndStatusNot(
                            tp.getId(), EventStatus.ARCHIVED);
                    return TermPlanResponse.from(tp, used);
                })
                .toList();
    }
}
