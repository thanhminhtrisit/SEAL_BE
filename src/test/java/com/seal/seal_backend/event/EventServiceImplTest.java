package com.seal.seal_backend.event;

import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.enums.ResourceType;
import com.seal.seal_backend.domain.enums.TermType;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import com.seal.seal_backend.capacity.CapacityService;
import com.seal.seal_backend.event.service.impl.EventServiceImpl;
import com.seal.seal_backend.shared.contract.JudgeQueryPort;
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
    @Mock CategoryRepository categoryRepository;
    @Mock JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock EventBudgetRepository eventBudgetRepository;
    @Mock TeamRepository teamRepository;
    @Mock CategoryResourceRepository categoryResourceRepository;
    @Mock CapacityService capacityService;
    @Mock AuditPublisher auditPublisher;
    @Mock JudgeQueryPort judgeQueryPort;

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
        sampleTermPlan.setTerm(TermType.SUMMER);
        sampleTermPlan.setDiscipline(sampleDiscipline);

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

            CreateRoundRequest req = new CreateRoundRequest("Round 1", 1, null, null, null, false, null, null, null, null);

            assertThatThrownBy(() -> service.addRound(1L, req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-02");
        }

        @Test
        void eventNotFound_throws_ResourceNotFound() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addRound(99L,
                    new CreateRoundRequest("R1", 1, null, null, null, false, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Criteria ─────────────────────────────────────────────────────────────

    @Nested
    class AddCriteriaSet {

        private CreateCriteriaSetRequest reqWith(BigDecimal w1, BigDecimal w2) {
            return new CreateCriteriaSetRequest("Set A", null, null, null, null, List.of(
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
        void categoryFromDifferentEvent_throws_BR_EVT_14() {
            Event otherEvent = new Event();
            otherEvent.setId(99L);
            Category foreignCat = new Category();
            foreignCat.setId(10L);
            foreignCat.setEvent(otherEvent);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(foreignCat));

            CreateCriteriaSetRequest req = new CreateCriteriaSetRequest(
                    "X", null, null, 10L, null,
                    List.of(new AddCriterionRequest("C1", null, new BigDecimal("10"), new BigDecimal("100"), 1)));

            assertThatThrownBy(() -> service.addCriteriaSet(1L, req, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-14");
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

    // ─── Judge Assignment ─────────────────────────────────────────────────────

    @Nested
    class JudgeAssignment {

        private Event event;
        private Round round;
        private User judge;
        private User coordinator;
        private Role judgeRole;

        @BeforeEach
        void setup() {
            judgeRole = new Role();
            judgeRole.setId(4L);
            judgeRole.setCode("JUDGE");

            event = new Event();
            event.setId(1L);

            round = new Round();
            round.setId(10L);
            round.setEvent(event);

            judge = new User();
            judge.setId(20L);
            judge.setPrimaryRole(judgeRole);

            coordinator = new User();
            coordinator.setId(99L);
        }

        @Test
        void assignJudge_duplicate_throws_BR_JDG_02() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
            when(userRepository.findById(20L)).thenReturn(Optional.of(judge));
            when(judgeAssignmentRepository.existsByJudgeIdAndRoundIdAndStatus(
                    20L, 10L, AssignmentStatus.ACTIVE)).thenReturn(true);

            assertThatThrownBy(() -> service.assignJudge(1L, 10L,
                    new AssignJudgeRequest(20L, null), 99L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-JDG-02");
        }

        @Test
        void assignJudge_nonJudgeUser_throws_BR_JDG_01() {
            Role memberRole = new Role();
            memberRole.setId(7L);
            memberRole.setCode("TEAM_MEMBER");
            judge.setPrimaryRole(memberRole);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
            when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
            when(userRepository.findById(20L)).thenReturn(Optional.of(judge));

            assertThatThrownBy(() -> service.assignJudge(1L, 10L,
                    new AssignJudgeRequest(20L, null), 99L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-JDG-01");
        }
    }

    // ─── Submit Event ─────────────────────────────────────────────────────────

    @Nested
    class SubmitEvent {

        private Round round;
        private CriteriaSet cs;
        private ScoringCriterion crit;
        private Category category;

        @BeforeEach
        void setup() {
            round = new Round();
            round.setId(10L);
            round.setName("Round 1");
            round.setEvent(sampleEvent);

            cs = new CriteriaSet();
            cs.setId(5L);
            cs.setEvent(sampleEvent);

            crit = new ScoringCriterion();
            crit.setId(1L);
            crit.setWeight(new BigDecimal("100"));

            category = new Category();
            category.setId(20L);
            category.setEvent(sampleEvent);
        }

        @Test
        void missingRound_throws_BR_EVT_08() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findByEventIdOrderByOrderNumber(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.submitEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-08");
        }

        @Test
        void missingJudge_throws_BR_EVT_06() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findByEventIdOrderByOrderNumber(1L)).thenReturn(List.of(round));
            when(criteriaSetRepository.findFirstByRoundId(10L)).thenReturn(Optional.of(cs));
            when(scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(5L))
                    .thenReturn(List.of(crit));
            when(judgeQueryPort.judgeIdsForRound(10L)).thenReturn(List.of()); // no judges

            assertThatThrownBy(() -> service.submitEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-06");
        }

        @Test
        void missingBudget_throws_BR_EVT_09() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findByEventIdOrderByOrderNumber(1L)).thenReturn(List.of(round));
            when(criteriaSetRepository.findFirstByRoundId(10L)).thenReturn(Optional.of(cs));
            when(scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(5L))
                    .thenReturn(List.of(crit));
            when(judgeQueryPort.judgeIdsForRound(10L)).thenReturn(List.of(99L));
            when(eventBudgetRepository.findByEventId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-09");
        }

        @Test
        void completeEvent_setsPendingApprovalAndAudits() {
            EventBudget budget = new EventBudget();
            budget.setId(1L);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findByEventIdOrderByOrderNumber(1L)).thenReturn(List.of(round));
            when(criteriaSetRepository.findFirstByRoundId(10L)).thenReturn(Optional.of(cs));
            when(scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(5L))
                    .thenReturn(List.of(crit));
            when(judgeQueryPort.judgeIdsForRound(10L)).thenReturn(List.of(99L));
            when(eventBudgetRepository.findByEventId(1L)).thenReturn(Optional.of(budget));
            when(categoryRepository.findByEventIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(category));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            EventResponse resp = service.submitEvent(1L, 3L);

            assertThat(resp.status()).isEqualTo(EventStatus.PENDING_APPROVAL);
            verify(eventRepository).save(argThat(e ->
                    e.getStatus() == EventStatus.PENDING_APPROVAL
                    && e.getSubmittedAt() != null));
            verify(auditPublisher).log(eq(sampleUser), eq(AuditAction.EVENT_SUBMITTED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }

        @Test
        void notOwner_throws_ForbiddenActionException() {
            User otherUser = new User();
            otherUser.setId(999L);
            // sampleEvent's owner is sampleUser (id=3), submitting as user 999
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.submitEvent(1L, 999L))
                    .isInstanceOf(ForbiddenActionException.class);
        }
    }

    // ─── Approve / Reject Event (BR-GOV-02) ──────────────────────────────────

    @Nested
    class ApproveRejectEvent {

        private User superCoord;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            superCoord = new User();
            superCoord.setId(99L);
            superCoord.setEmail("super@seal.local");

            sampleEvent.setStatus(EventStatus.PENDING_APPROVAL);
        }

        // ── approve ──────────────────────────────────────────────────────────

        @Test
        void approve_pendingEvent_setsApprovedAndAudits() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(99L)).thenReturn(Optional.of(superCoord));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventResponse resp = service.approveEvent(1L, 99L);

            assertThat(resp.status()).isEqualTo(EventStatus.APPROVED);
            verify(eventRepository).save(argThat(e ->
                    e.getStatus() == EventStatus.APPROVED
                    && e.getApprovedBy() != null
                    && e.getApprovedAt() != null));
            verify(auditPublisher).log(eq(superCoord), eq(AuditAction.EVENT_APPROVED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }

        @Test
        void approve_notPending_throws_BR_GOV_01() {
            sampleEvent.setStatus(EventStatus.DRAFT);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.approveEvent(1L, 99L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-01");
        }

        @Test
        void approve_sameAsOwner_throws_BR_GOV_02() {
            // sampleUser (id=3) is ownerCoordinator; approverId=3 → separation of duties violation
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.approveEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-02");
        }

        // ── reject ───────────────────────────────────────────────────────────

        @Test
        void reject_pendingEvent_setsRejectedAndAudits() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(userRepository.findById(99L)).thenReturn(Optional.of(superCoord));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventResponse resp = service.rejectEvent(1L, 99L, "Incomplete criteria");

            assertThat(resp.status()).isEqualTo(EventStatus.REJECTED);
            verify(eventRepository).save(argThat(e ->
                    e.getStatus() == EventStatus.REJECTED
                    && "Incomplete criteria".equals(e.getRejectionReason())));
            verify(auditPublisher).log(eq(superCoord), eq(AuditAction.EVENT_REJECTED),
                    eq("EVENT"), eq(1L), any(), any(), eq("Incomplete criteria"), isNull());
        }

        @Test
        void reject_notPending_throws_BR_GOV_01() {
            sampleEvent.setStatus(EventStatus.APPROVED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.rejectEvent(1L, 99L, "reason"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-01");
        }

        @Test
        void reject_sameAsOwner_throws_BR_GOV_02() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.rejectEvent(1L, 3L, "reason"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-02");
        }

        @Test
        void reject_blankReason_throws_BR_GOV_03() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.rejectEvent(1L, 99L, "  "))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-GOV-03");
        }
    }

    // ─── Pending Lock ─────────────────────────────────────────────────────────

    @Nested
    class PendingLock {

        @Test
        void updateEvent_pendingApproval_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.PENDING_APPROVAL);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.update(1L,
                    new UpdateEventRequest("New Name", null, null, null, null, null, null, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }

        @Test
        void addRound_pendingApproval_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.PENDING_APPROVAL);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.addRound(1L,
                    new CreateRoundRequest("R1", 1, null, null, null, false, null, null, null, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── Criteria CRUD ────────────────────────────────────────────────────────

    @Nested
    class CriteriaSetMutations {

        private CriteriaSet cs;
        private ScoringCriterion crit;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            cs = new CriteriaSet();
            cs.setId(5L);
            cs.setName("Original");
            cs.setEvent(sampleEvent);

            crit = new ScoringCriterion();
            crit.setId(1L);
            crit.setCriteriaSet(cs);
            crit.setWeight(new BigDecimal("100"));
            crit.setDisplayOrder(1);
        }

        @Test
        void updateCriteriaSet_approvedEvent_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.APPROVED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.updateCriteriaSet(1L, 5L,
                    new UpdateCriteriaSetRequest("New Name", null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }

        @Test
        void updateCriteriaSet_draft_updatesNameAndDescription() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(criteriaSetRepository.findById(5L)).thenReturn(Optional.of(cs));
            when(criteriaSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(5L))
                    .thenReturn(List.of(crit));

            CriteriaSetResponse resp = service.updateCriteriaSet(1L, 5L,
                    new UpdateCriteriaSetRequest("New Name", "desc"));

            assertThat(resp.name()).isEqualTo("New Name");
        }

        @Test
        void replaceCriteria_badWeight_throws_BR_EVT_03() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(criteriaSetRepository.findById(5L)).thenReturn(Optional.of(cs));

            ReplaceCriteriaRequest req = new ReplaceCriteriaRequest(List.of(
                    new AddCriterionRequest("C1", null, new BigDecimal("10"), new BigDecimal("50"), 1)
            ));
            assertThatThrownBy(() -> service.replaceCriteria(1L, 5L, req))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-03");
        }

        @Test
        void deleteCriteriaSet_draft_deletesSetAndCriteria() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(criteriaSetRepository.findById(5L)).thenReturn(Optional.of(cs));
            when(scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(5L))
                    .thenReturn(List.of(crit));

            service.deleteCriteriaSet(1L, 5L);

            verify(scoringCriterionRepository).deleteAll(List.of(crit));
            verify(criteriaSetRepository).delete(cs);
        }

        @Test
        void deleteCriteriaSet_approvedEvent_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.OPEN);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.deleteCriteriaSet(1L, 5L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── Delete Round ─────────────────────────────────────────────────────────

    @Nested
    class DeleteRound {

        private Round round;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            round = new Round();
            round.setId(10L);
            round.setEvent(sampleEvent);
            round.setStatus(RoundStatus.DRAFT);
        }

        @Test
        void deleteRound_hasCriteriaSet_throws_BR_EVT_12() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
            when(criteriaSetRepository.existsByRoundId(10L)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteRound(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-12");
        }

        @Test
        void deleteRound_hasJudges_throws_BR_EVT_12() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
            when(criteriaSetRepository.existsByRoundId(10L)).thenReturn(false);
            when(judgeAssignmentRepository.existsByRoundId(10L)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteRound(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-12");
        }

        @Test
        void deleteRound_noDependencies_deletesSuccessfully() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
            when(criteriaSetRepository.existsByRoundId(10L)).thenReturn(false);
            when(judgeAssignmentRepository.existsByRoundId(10L)).thenReturn(false);

            service.deleteRound(1L, 10L);

            verify(roundRepository).deleteById(10L);
        }

        @Test
        void deleteRound_approvedEvent_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.IN_PROGRESS);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.deleteRound(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── Delete Category ──────────────────────────────────────────────────────

    @Nested
    class DeleteCategory {

        private Category category;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            category = new Category();
            category.setId(20L);
            category.setEvent(sampleEvent);
            category.setIsActive(true);
        }

        @Test
        void deleteCategory_teamsRegistered_throws_BR_EVT_13() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
            when(teamRepository.existsByCategoryId(20L)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteCategory(1L, 20L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-13");
        }

        @Test
        void deleteCategory_noTeams_deletesSuccessfully() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
            when(teamRepository.existsByCategoryId(20L)).thenReturn(false);

            service.deleteCategory(1L, 20L);

            verify(categoryRepository).deleteById(20L);
        }

        @Test
        void deleteCategory_approvedEvent_throws_BR_EVT_07() {
            sampleEvent.setStatus(EventStatus.APPROVED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.deleteCategory(1L, 20L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-07");
        }
    }

    // ─── Category Resources ───────────────────────────────────────────────────

    @Nested
    class CategoryResources {

        private Category sampleCategory;

        @org.junit.jupiter.api.BeforeEach
        void setup() {
            sampleCategory = new Category();
            sampleCategory.setId(10L);
            sampleCategory.setEvent(sampleEvent);
            sampleCategory.setName("Web Application");
            sampleCategory.setIsActive(true);
        }

        @Test
        void addResource_validRequest_savesAndReturns() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));

            CategoryResource saved = new CategoryResource();
            saved.setId(1L);
            saved.setCategory(sampleCategory);
            saved.setUrl("https://dataset.example.com/data.csv");
            saved.setResourceType(ResourceType.DATASET);
            when(categoryResourceRepository.save(any())).thenReturn(saved);

            var req = new CreateCategoryResourceRequest(null, "https://dataset.example.com/data.csv", ResourceType.DATASET);
            var resp = service.addCategoryResource(1L, 10L, req);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.resourceType()).isEqualTo(ResourceType.DATASET);
            verify(categoryResourceRepository).save(any());
        }

        @Test
        void addResource_categoryWrongEvent_throws_BR_RES_01() {
            Event otherEvent = new Event();
            otherEvent.setId(99L);
            Category foreignCat = new Category();
            foreignCat.setId(10L);
            foreignCat.setEvent(otherEvent);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(foreignCat));

            assertThatThrownBy(() -> service.addCategoryResource(1L, 10L,
                    new CreateCategoryResourceRequest(null, "https://x.com", ResourceType.LINK)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-RES-01");
        }

        @Test
        void listResources_returnsAll() {
            CategoryResource r1 = new CategoryResource();
            r1.setId(1L); r1.setCategory(sampleCategory);
            r1.setUrl("https://a.com"); r1.setResourceType(ResourceType.DOC);
            CategoryResource r2 = new CategoryResource();
            r2.setId(2L); r2.setCategory(sampleCategory);
            r2.setUrl("https://b.com"); r2.setResourceType(ResourceType.SAMPLE);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
            when(categoryResourceRepository.findByCategoryIdOrderByCreatedAtAsc(10L))
                    .thenReturn(List.of(r1, r2));

            var result = service.listCategoryResources(1L, 10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).resourceType()).isEqualTo(ResourceType.DOC);
        }

        @Test
        void deleteResource_wrongCategory_throws_BR_RES_02() {
            Category otherCat = new Category();
            otherCat.setId(99L);
            otherCat.setEvent(sampleEvent);

            CategoryResource resource = new CategoryResource();
            resource.setId(5L);
            resource.setCategory(otherCat);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
            when(categoryResourceRepository.findById(5L)).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> service.deleteCategoryResource(1L, 10L, 5L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-RES-02");
        }

        @Test
        void deleteResource_valid_callsDeleteById() {
            CategoryResource resource = new CategoryResource();
            resource.setId(5L);
            resource.setCategory(sampleCategory);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
            when(categoryResourceRepository.findById(5L)).thenReturn(Optional.of(resource));

            service.deleteCategoryResource(1L, 10L, 5L);

            verify(categoryResourceRepository).deleteById(5L);
        }
    }

    // ─── Mentor Planning ──────────────────────────────────────────────────────

    @Nested
    class MentorPlanning {

        @Test
        void correctCalculation_withGap() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.countActiveTeamsByEventId(1L)).thenReturn(10L);
            when(capacityService.effectiveMaxTeamsPerMentor(sampleEvent)).thenReturn(5);
            when(categoryRepository.countDistinctMentorsByEventId(1L)).thenReturn(1L);

            MentorPlanningResponse resp = service.getMentorPlanning(1L);

            assertThat(resp.activeTeams()).isEqualTo(10L);
            assertThat(resp.maxTeamsPerMentor()).isEqualTo(5);
            assertThat(resp.mentorsNeeded()).isEqualTo(2); // ceil(10/5)
            assertThat(resp.currentMentors()).isEqualTo(1L);
            assertThat(resp.gap()).isEqualTo(1); // 2 - 1
        }

        @Test
        void noGap_whenEnoughMentors() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.countActiveTeamsByEventId(1L)).thenReturn(8L);
            when(capacityService.effectiveMaxTeamsPerMentor(sampleEvent)).thenReturn(5);
            when(categoryRepository.countDistinctMentorsByEventId(1L)).thenReturn(2L);

            MentorPlanningResponse resp = service.getMentorPlanning(1L);

            assertThat(resp.mentorsNeeded()).isEqualTo(2); // ceil(8/5)=2
            assertThat(resp.currentMentors()).isEqualTo(2L);
            assertThat(resp.gap()).isEqualTo(0);
        }

        @Test
        void gapIsNeverNegative_whenMoreMentorsThanNeeded() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.countActiveTeamsByEventId(1L)).thenReturn(3L);
            when(capacityService.effectiveMaxTeamsPerMentor(sampleEvent)).thenReturn(5);
            when(categoryRepository.countDistinctMentorsByEventId(1L)).thenReturn(5L);

            MentorPlanningResponse resp = service.getMentorPlanning(1L);

            assertThat(resp.mentorsNeeded()).isEqualTo(1); // ceil(3/5)=1
            assertThat(resp.currentMentors()).isEqualTo(5L);
            assertThat(resp.gap()).isEqualTo(0); // max(0, 1-5)=0
        }

        @Test
        void noActiveTeams_returnsZeroNeeded() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.countActiveTeamsByEventId(1L)).thenReturn(0L);
            when(capacityService.effectiveMaxTeamsPerMentor(sampleEvent)).thenReturn(5);
            when(categoryRepository.countDistinctMentorsByEventId(1L)).thenReturn(0L);

            MentorPlanningResponse resp = service.getMentorPlanning(1L);

            assertThat(resp.mentorsNeeded()).isEqualTo(0);
            assertThat(resp.gap()).isEqualTo(0);
        }
    }

    // ─── listAll with status filter ───────────────────────────────────────────

    @Nested
    class ListAllEvents {

        @Test
        void noFilter_returnsAllEvents() {
            Event e2 = new Event();
            e2.setId(2L);
            e2.setName("Open Event");
            e2.setStatus(EventStatus.OPEN);
            e2.setOwnerCoordinator(sampleUser);

            when(eventRepository.findAllByOrderByCreatedAtDesc())
                    .thenReturn(List.of(sampleEvent, e2));

            var result = service.listAll(null);

            assertThat(result).hasSize(2);
        }

        @Test
        void filterApproved_returnsOnlyApprovedEvents() {
            Event approved = new Event();
            approved.setId(3L);
            approved.setName("Approved Event");
            approved.setStatus(EventStatus.APPROVED);
            approved.setOwnerCoordinator(sampleUser);

            when(eventRepository.findAllByStatusOrderByCreatedAtDesc(EventStatus.APPROVED))
                    .thenReturn(List.of(approved));

            var result = service.listAll(EventStatus.APPROVED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(EventStatus.APPROVED);
        }

        @Test
        void filterReturnsEmpty_whenNoMatch() {
            when(eventRepository.findAllByStatusOrderByCreatedAtDesc(EventStatus.COMPLETED))
                    .thenReturn(List.of());

            assertThat(service.listAll(EventStatus.COMPLETED)).isEmpty();
        }
    }

    // ─── Lifecycle transitions ────────────────────────────────────────────────

    @Nested
    class Lifecycle {

        @Test
        void open_whenNotApproved_throws_BR_EVT_11() {
            // sampleEvent is DRAFT — cannot open
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.openEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-11");
        }

        @Test
        void start_whenNotOpen_throws_BR_EVT_11() {
            sampleEvent.setStatus(EventStatus.DRAFT);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

            assertThatThrownBy(() -> service.startEvent(1L, 3L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-EVT-11");
        }

        @Test
        void open_whenApproved_setsOpenAndAudits() {
            sampleEvent.setStatus(EventStatus.APPROVED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            EventResponse resp = service.openEvent(1L, 3L);

            assertThat(resp.status()).isEqualTo(EventStatus.OPEN);
            verify(auditPublisher).log(eq(sampleUser), eq(AuditAction.EVENT_OPENED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }

        @Test
        void start_whenOpen_setsInProgress() {
            sampleEvent.setStatus(EventStatus.OPEN);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            EventResponse resp = service.startEvent(1L, 3L);

            assertThat(resp.status()).isEqualTo(EventStatus.IN_PROGRESS);
            verify(auditPublisher).log(eq(sampleUser), eq(AuditAction.EVENT_STARTED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }

        @Test
        void complete_whenInProgress_setsCompleted() {
            sampleEvent.setStatus(EventStatus.IN_PROGRESS);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            EventResponse resp = service.completeEvent(1L, 3L);

            assertThat(resp.status()).isEqualTo(EventStatus.COMPLETED);
            verify(auditPublisher).log(eq(sampleUser), eq(AuditAction.EVENT_COMPLETED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }

        @Test
        void archive_whenCompleted_setsArchivedAndSetsTimestamp() {
            sampleEvent.setStatus(EventStatus.COMPLETED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleUser));

            EventResponse resp = service.archiveEvent(1L, 3L);

            assertThat(resp.status()).isEqualTo(EventStatus.ARCHIVED);
            verify(eventRepository).save(argThat(e ->
                    e.getStatus() == EventStatus.ARCHIVED
                    && e.getArchivedAt() != null));
            verify(auditPublisher).log(eq(sampleUser), eq(AuditAction.EVENT_ARCHIVED),
                    eq("EVENT"), eq(1L), any(), any(), isNull(), isNull());
        }
    }
}
