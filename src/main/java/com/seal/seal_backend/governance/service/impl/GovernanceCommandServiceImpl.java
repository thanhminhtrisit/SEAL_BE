package com.seal.seal_backend.governance.service.impl;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.Discipline;
import com.seal.seal_backend.domain.entity.TermPlan;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.TermType;
import com.seal.seal_backend.domain.repository.DisciplineRepository;
import com.seal.seal_backend.domain.repository.EventRepository;
import com.seal.seal_backend.domain.repository.TermPlanRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import com.seal.seal_backend.governance.dto.request.CreateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.CreateTermPlanRequest;
import com.seal.seal_backend.governance.dto.request.UpdateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.UpdateTermPlanRequest;
import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;
import com.seal.seal_backend.governance.service.GovernanceCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GovernanceCommandServiceImpl implements GovernanceCommandService {

    private final DisciplineRepository disciplineRepository;
    private final TermPlanRepository termPlanRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DisciplineResponse createDiscipline(CreateDisciplineRequest req, Long actorId) {
        String code = req.code() == null ? "" : req.code().trim();
        String name = req.name() == null ? "" : req.name().trim();
        if (code.isEmpty() || name.isEmpty()) {
            throw new BusinessRuleException("FR-GOV-01", "Discipline code and name are required");
        }
        if (disciplineRepository.existsByCode(code)) {
            throw new BusinessRuleException("FR-GOV-01", "Discipline code already exists: " + code);
        }
        Discipline d = new Discipline();
        d.setCode(code);
        d.setName(name);
        d.setDescription(req.description());
        d.setIsActive(true);
        d.setCreatedBy(userRepository.getReferenceById(actorId));
        return DisciplineResponse.from(disciplineRepository.save(d));
    }

    @Override
    @Transactional
    public DisciplineResponse updateDiscipline(Long id, UpdateDisciplineRequest req, Long actorId) {
        Discipline d = disciplineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discipline", id));
        if (req.name() != null && !req.name().trim().isEmpty()) {
            d.setName(req.name().trim());
        }
        if (req.description() != null) {
            d.setDescription(req.description());
        }
        if (req.isActive() != null) {
            d.setIsActive(req.isActive());
        }
        return DisciplineResponse.from(disciplineRepository.save(d));
    }

    @Override
    @Transactional
    public TermPlanResponse createTermPlan(CreateTermPlanRequest req, Long actorId) {
        if (req.term() == null || req.year() == null || req.disciplineId() == null) {
            throw new BusinessRuleException("FR-GOV-02", "term, year and disciplineId are required");
        }
        TermType term;
        try {
            term = TermType.valueOf(req.term().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("FR-GOV-02", "Invalid term (SPRING/SUMMER/FALL): " + req.term());
        }
        Discipline d = disciplineRepository.findById(req.disciplineId())
                .orElseThrow(() -> new ResourceNotFoundException("Discipline", req.disciplineId()));
        if (termPlanRepository.existsByTermAndYearAndDiscipline_Id(term, req.year(), d.getId())) {
            throw new BusinessRuleException("FR-GOV-02",
                    "A quota already exists for " + term + " " + req.year() + " / " + d.getName());
        }
        int max = req.maxEvents() == null ? 1 : req.maxEvents();
        if (max < 1) {
            throw new BusinessRuleException("FR-GOV-02", "maxEvents must be at least 1");
        }
        TermPlan tp = new TermPlan();
        tp.setTerm(term);
        tp.setYear(req.year());
        tp.setDiscipline(d);
        tp.setMaxEvents(max);
        tp.setCreatedBy(userRepository.getReferenceById(actorId));
        return TermPlanResponse.from(termPlanRepository.save(tp), 0L);
    }

    @Override
    @Transactional
    public TermPlanResponse updateTermPlan(Long id, UpdateTermPlanRequest req, Long actorId) {
        TermPlan tp = termPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TermPlan", id));
        if (req.maxEvents() == null || req.maxEvents() < 1) {
            throw new BusinessRuleException("FR-GOV-02", "maxEvents must be at least 1");
        }
        tp.setMaxEvents(req.maxEvents());
        TermPlan saved = termPlanRepository.save(tp);
        long used = eventRepository.countByTermPlanIdAndStatusNot(saved.getId(), EventStatus.ARCHIVED);
        return TermPlanResponse.from(saved, used);
    }
}
