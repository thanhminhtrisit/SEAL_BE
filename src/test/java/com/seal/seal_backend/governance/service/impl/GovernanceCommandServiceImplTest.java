package com.seal.seal_backend.governance.service.impl;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.domain.entity.Discipline;
import com.seal.seal_backend.domain.entity.TermPlan;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.TermType;
import com.seal.seal_backend.domain.repository.DisciplineRepository;
import com.seal.seal_backend.domain.repository.EventRepository;
import com.seal.seal_backend.domain.repository.TermPlanRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import com.seal.seal_backend.governance.dto.request.CreateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.CreateTermPlanRequest;
import com.seal.seal_backend.governance.dto.request.UpdateTermPlanRequest;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceCommandServiceImplTest {

    @Mock DisciplineRepository disciplineRepository;
    @Mock TermPlanRepository termPlanRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;

    @InjectMocks GovernanceCommandServiceImpl service;

    private Discipline discipline(long id, String name) {
        Discipline d = new Discipline();
        d.setId(id);
        d.setName(name);
        return d;
    }

    // ─────────────────────── createDiscipline ─────────────────────────────

    @Test
    void createDiscipline_duplicateCode_throwsBusinessRule() {
        when(disciplineRepository.existsByCode("SE")).thenReturn(true);

        assertThatThrownBy(() -> service.createDiscipline(
                new CreateDisciplineRequest("SE", "Software", null), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createDiscipline_unique_constraint_race_mapsTo409BusinessRule() {
        when(disciplineRepository.existsByCode("SE")).thenReturn(false);
        when(disciplineRepository.save(any(Discipline.class)))
                .thenThrow(new DataIntegrityViolationException("uq_disciplines_code"));

        assertThatThrownBy(() -> service.createDiscipline(
                new CreateDisciplineRequest("SE", "Software", null), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    // ─────────────────────── createTermPlan ───────────────────────────────

    @Test
    void createTermPlan_invalidTermType_throwsBusinessRule() {
        assertThatThrownBy(() -> service.createTermPlan(
                new CreateTermPlanRequest("WINTER", 2026, 5L, 2), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid term");
    }

    @Test
    void createTermPlan_maxEventsBelowOne_throwsBusinessRule() {
        when(disciplineRepository.findById(5L)).thenReturn(Optional.of(discipline(5L, "SE")));
        when(termPlanRepository.existsByTermAndYearAndDiscipline_Id(TermType.SPRING, 2026, 5L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createTermPlan(
                new CreateTermPlanRequest("SPRING", 2026, 5L, 0), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void createTermPlan_duplicate_race_mapsTo409BusinessRule() {
        when(disciplineRepository.findById(5L)).thenReturn(Optional.of(discipline(5L, "SE")));
        when(termPlanRepository.existsByTermAndYearAndDiscipline_Id(TermType.SPRING, 2026, 5L))
                .thenReturn(false);
        when(termPlanRepository.save(any(TermPlan.class)))
                .thenThrow(new DataIntegrityViolationException("uq_term_plans_term_year_discipline"));

        assertThatThrownBy(() -> service.createTermPlan(
                new CreateTermPlanRequest("SPRING", 2026, 5L, 3), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    // ─────────────────────── updateTermPlan ───────────────────────────────

    @Test
    void updateTermPlan_returnsUsedAndRemaining() {
        TermPlan tp = new TermPlan();
        tp.setId(9L);
        tp.setTerm(TermType.SPRING);
        tp.setYear(2026);
        tp.setDiscipline(discipline(5L, "SE"));
        tp.setMaxEvents(1);

        when(termPlanRepository.findById(9L)).thenReturn(Optional.of(tp));
        when(termPlanRepository.save(any(TermPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.countByTermPlanIdAndStatusNot(eq(9L), eq(EventStatus.ARCHIVED)))
                .thenReturn(3L);

        TermPlanResponse res = service.updateTermPlan(9L, new UpdateTermPlanRequest(5), 1L);

        assertThat(res.maxEvents()).isEqualTo(5);
        assertThat(res.usedEvents()).isEqualTo(3L);
        assertThat(res.remaining()).isEqualTo(2L);
    }
}
