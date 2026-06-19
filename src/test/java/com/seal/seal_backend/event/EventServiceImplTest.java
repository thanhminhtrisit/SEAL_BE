package com.seal.seal_backend.event;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import com.seal.seal_backend.event.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock RoundRepository roundRepository;
    @Mock CriteriaSetRepository criteriaSetRepository;
    @Mock ScoringCriterionRepository scoringCriterionRepository;
    @Mock DisciplineRepository disciplineRepository;
    @Mock TermPlanRepository termPlanRepository;
    @Mock UserRepository userRepository;

    @InjectMocks EventServiceImpl service;

    private Event sampleEvent;
    private User sampleUser;
    private Discipline sampleDiscipline;
    private TermPlan sampleTermPlan;

    @BeforeEach
    void setup() {
        sampleDiscipline = new Discipline();
        sampleDiscipline.setId(1L);
        sampleDiscipline.setName("Software Engineering");

        sampleTermPlan = new TermPlan();
        sampleTermPlan.setId(1L);

        sampleUser = new User();
        sampleUser.setId(3L);
        sampleUser.setEmail("coord@seal.local");

        sampleEvent = new Event();
        sampleEvent.setId(1L);
        sampleEvent.setName("Test Event");
        sampleEvent.setSlug("test-event");
        sampleEvent.setEventType(EventType.SUMMER);
        sampleEvent.setDiscipline(sampleDiscipline);
        sampleEvent.setTermPlan(sampleTermPlan);
        sampleEvent.setStatus(EventStatus.DRAFT);
        sampleEvent.setOwnerCoordinator(sampleUser);
        sampleEvent.setCreatedBy(sampleUser);
    }

    // ─── Event creation ───────────────────────────────────────────────────────

    @Nested
    class CreateEvent {

        @Test
        void invalidRegistrationWindow_throws_BR_EVT_01() {
            CreateEventRequest req = new CreateEventRequest(
                    "My Event", 1L, 1L, EventType.SUMMER, null,
                    LocalDateTime.of(2026, 7, 10, 0, 0),
                    LocalDateTime.of(2026, 7, 5, 0, 0)  // end before start
            );

            assertThatThrownBy(() -> service.create(req, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-01");
        }

        @Test
        void disciplineNotFound_throws_ResourceNotFound() {
            CreateEventRequest req = new CreateEventRequest(
                    "My Event", 99L, 1L, EventType.SUMMER, null, null, null);
            when(disciplineRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(req, 3L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void validRequest_savesAndReturns() {
            CreateEventRequest req = new CreateEventRequest(
                    "SEAL Summer 2026", 1L, 1L, EventType.SUMMER, "desc",
                    LocalDateTime.of(2026, 6, 1, 0, 0),
                    LocalDateTime.of(2026, 6, 20, 0, 0));

            when(disciplineRepository.findById(1L)).thenReturn(Optional.of(sampleDiscipline));
            when(termPlanRepository.findById(1L)).thenReturn(Optional.of(sampleTermPlan));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));
            when(eventRepository.existsBySlug(any())).thenReturn(false);
            when(eventRepository.save(any())).thenReturn(sampleEvent);

            EventResponse resp = service.create(req, 3L);

            assertThat(resp.id()).isEqualTo(1L);
            verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.DRAFT));
        }
    }

    // ─── Round ────────────────────────────────────────────────────────────────

    @Nested
    class AddRound {

        @Test
        void duplicateOrderNumber_throws_BR_EVT_02() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.existsByEventIdAndOrderNumber(1L, 1)).thenReturn(true);

            CreateRoundRequest req = new CreateRoundRequest("Round 1", 1, null, null, false);

            assertThatThrownBy(() -> service.addRound(1L, req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-02");
        }

        @Test
        void eventNotFound_throws_ResourceNotFound() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addRound(99L,
                    new CreateRoundRequest("R1", 1, null, null, false)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Criteria ─────────────────────────────────────────────────────────────

    @Nested
    class AddCriteriaSet {

        private CreateCriteriaSetRequest reqWith(BigDecimal w1, BigDecimal w2) {
            return new CreateCriteriaSetRequest("Set A", null, null, List.of(
                    new AddCriterionRequest("C1", null, new BigDecimal("10"), w1, 1),
                    new AddCriterionRequest("C2", null, new BigDecimal("10"), w2, 2)
            ));
        }

        @Test
        void weightsNotSumTo100_throws_BR_EVT_03() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            // 40 + 40 = 80, not 100
            assertThatThrownBy(() -> service.addCriteriaSet(1L, reqWith(
                    new BigDecimal("40"), new BigDecimal("40")), 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-03");
        }

        @Test
        void weightsSumOver100_throws_BR_EVT_03() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            // 60 + 60 = 120
            assertThatThrownBy(() -> service.addCriteriaSet(1L, reqWith(
                    new BigDecimal("60"), new BigDecimal("60")), 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-03");
        }

        @Test
        void weightsExactly100_savesSuccessfully() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            CriteriaSet savedSet = new CriteriaSet();
            savedSet.setId(1L);
            savedSet.setName("Set A");
            savedSet.setEvent(sampleEvent);
            when(criteriaSetRepository.save(any())).thenReturn(savedSet);

            ScoringCriterion sc1 = criterionFor(savedSet, "C1", new BigDecimal("40"), 1);
            ScoringCriterion sc2 = criterionFor(savedSet, "C2", new BigDecimal("60"), 2);
            when(scoringCriterionRepository.save(any()))
                    .thenReturn(sc1).thenReturn(sc2);

            // 40 + 60 = 100
            CriteriaSetResponse resp = service.addCriteriaSet(1L, reqWith(
                    new BigDecimal("40"), new BigDecimal("60")), 3L);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.criteria()).hasSize(2);
            verify(criteriaSetRepository).save(any());
        }

        private ScoringCriterion criterionFor(CriteriaSet cs, String name, BigDecimal weight, int order) {
            ScoringCriterion sc = new ScoringCriterion();
            sc.setId((long) order);
            sc.setCriteriaSet(cs);
            sc.setName(name);
            sc.setWeight(weight);
            sc.setMaxScore(new BigDecimal("10"));
            sc.setDisplayOrder(order);
            sc.setIsActive(true);
            return sc;
        }
    }
}
