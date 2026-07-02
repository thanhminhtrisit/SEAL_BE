package com.seal.seal_backend.event.service.impl;

import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.domain.repository.TeamRepository;
import com.seal.seal_backend.event.dto.response.*;
import com.seal.seal_backend.event.service.EventService;
import com.seal.seal_backend.shared.contract.JudgeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final CriteriaSetRepository criteriaSetRepository;
    private final ScoringCriterionRepository scoringCriterionRepository;
    private final DisciplineRepository disciplineRepository;
    private final TermPlanRepository termPlanRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final EventBudgetRepository eventBudgetRepository;
    private final TeamRepository teamRepository;
    private final AuditPublisher auditPublisher;
    private final JudgeQueryPort judgeQueryPort;

    // ─── Event ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest req, Long creatorId) {
        if (req.registrationStart() != null && req.registrationEnd() != null
                && !req.registrationStart().isBefore(req.registrationEnd())) {
            throw new BusinessRuleException("BR-EVT-01",
                    "registration_start must be before registration_end");
        }

        Discipline discipline = disciplineRepository.findById(req.disciplineId())
                .orElseThrow(() -> new ResourceNotFoundException("Discipline not found: " + req.disciplineId()));
        TermPlan termPlan = termPlanRepository.findById(req.termPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("TermPlan not found: " + req.termPlanId()));

        // Discipline must match term plan's discipline
        if (!discipline.getId().equals(termPlan.getDiscipline().getId())) {
            throw new BusinessRuleException("BR-EVT-05",
                    "Event discipline does not match the term plan's discipline.");
        }

        // Event type must match term plan's term (SPECIAL events are exempt)
        if (req.eventType() != EventType.SPECIAL
                && !req.eventType().name().equals(termPlan.getTerm().name())) {
            throw new BusinessRuleException("BR-EVT-05",
                    "Event type " + req.eventType()
                    + " does not match term plan term " + termPlan.getTerm() + ".");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        String slug = uniqueSlug(req.name());

        Event event = new Event();
        event.setName(req.name());
        event.setSlug(slug);
        event.setEventType(req.eventType());
        event.setDiscipline(discipline);
        event.setTermPlan(termPlan);
        event.setDescription(req.description());
        event.setRegistrationStart(req.registrationStart());
        event.setRegistrationEnd(req.registrationEnd());
        event.setStatus(EventStatus.DRAFT);
        event.setOwnerCoordinator(creator);
        event.setCreatedBy(creator);

        return EventResponse.from(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        return EventResponse.from(findEvent(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> listAll(EventStatus status) {
        List<Event> events = status == null
                ? eventRepository.findAllByOrderByCreatedAtDesc()
                : eventRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return events.stream().map(EventSummaryResponse::from).toList();
    }

    @Override
    @Transactional
    public EventResponse update(Long id, UpdateEventRequest req) {
        Event event = findEvent(id);
        validateNotPending(event);

        if (req.name() != null && !req.name().isBlank()) event.setName(req.name());
        if (req.description() != null) event.setDescription(req.description());
        if (req.registrationStart() != null) event.setRegistrationStart(req.registrationStart());
        if (req.registrationEnd() != null) event.setRegistrationEnd(req.registrationEnd());

        if (event.getRegistrationStart() != null && event.getRegistrationEnd() != null
                && !event.getRegistrationStart().isBefore(event.getRegistrationEnd())) {
            throw new BusinessRuleException("BR-EVT-01",
                    "registration_start must be before registration_end");
        }

        return EventResponse.from(eventRepository.save(event));
    }

    // ─── Submit (FR-EVT-07) ───────────────────────────────────────────────────

    @Override
    @Transactional
    public EventResponse submitEvent(Long eventId, Long coordinatorId) {
        Event event = findEvent(eventId);

        if (!event.getOwnerCoordinator().getId().equals(coordinatorId)) {
            throw new ForbiddenActionException("Only the owner coordinator can submit this event.");
        }

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED) {
            throw new BusinessRuleException("BR-EVT-06S",
                    "Only DRAFT or REJECTED events can be submitted. Current status: " + event.getStatus());
        }

        // BR-EVT-08: must have at least one round
        List<Round> rounds = roundRepository.findByEventIdOrderByOrderNumber(eventId);
        if (rounds.isEmpty()) {
            throw new BusinessRuleException("BR-EVT-08",
                    "Event must have at least one round before submission.");
        }

        for (Round round : rounds) {
            // BR-EVT-03: each round must have a criteria set with weights summing to 100
            CriteriaSet cs = criteriaSetRepository.findFirstByRoundId(round.getId())
                    .or(() -> criteriaSetRepository.findFirstByEventIdAndRoundIsNull(eventId))
                    .orElseThrow(() -> new BusinessRuleException("BR-EVT-03",
                            "Round '" + round.getName() + "' has no criteria set."));

            BigDecimal totalWeight = scoringCriterionRepository
                    .findByCriteriaSetIdOrderByDisplayOrder(cs.getId())
                    .stream()
                    .map(ScoringCriterion::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
                throw new BusinessRuleException("BR-EVT-03",
                        "Criteria weights for round '" + round.getName()
                        + "' must sum to 100 (got " + totalWeight + ").");
            }

            // BR-EVT-06: each round must have at least one judge
            if (judgeQueryPort.judgeIdsForRound(round.getId()).isEmpty()) {
                throw new BusinessRuleException("BR-EVT-06",
                        "Round '" + round.getName() + "' has no assigned judges.");
            }
        }

        // FR-EVT-08: event must have a budget
        if (eventBudgetRepository.findByEventId(eventId).isEmpty()) {
            throw new BusinessRuleException("BR-EVT-09",
                    "Event must have a budget before submission.");
        }

        // Must have at least one category
        if (categoryRepository.findByEventIdOrderByCreatedAtAsc(eventId).isEmpty()) {
            throw new BusinessRuleException("BR-EVT-10",
                    "Event must have at least one category before submission.");
        }

        String oldStatus = event.getStatus().name();
        event.setStatus(EventStatus.PENDING_APPROVAL);
        event.setSubmittedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        User coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + coordinatorId));
        auditPublisher.log(coordinator, AuditAction.EVENT_SUBMITTED, "EVENT", eventId,
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"PENDING_APPROVAL\"}", null, null);

        return EventResponse.from(saved);
    }

    // ─── Round ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoundResponse addRound(Long eventId, CreateRoundRequest req) {
        Event event = findEvent(eventId);
        validateNotPending(event);

        if (roundRepository.existsByEventIdAndOrderNumber(eventId, req.orderNumber())) {
            throw new BusinessRuleException("BR-EVT-02",
                    "A round with order_number " + req.orderNumber() + " already exists in this event");
        }

        Round round = new Round();
        round.setEvent(event);
        round.setName(req.name());
        round.setOrderNumber(req.orderNumber());
        round.setSubmissionDeadline(req.submissionDeadline());
        round.setScoringDeadline(req.scoringDeadline());
        round.setPromotionTopN(req.promotionTopN());
        round.setIsFinalRound(req.finalRound());
        round.setStatus(RoundStatus.DRAFT);
        if (req.requiresRepo() != null) round.setRequiresRepo(req.requiresRepo());
        if (req.requiresDemo() != null) round.setRequiresDemo(req.requiresDemo());
        if (req.requiresSlide() != null) round.setRequiresSlide(req.requiresSlide());
        if (req.requiresReport() != null) round.setRequiresReport(req.requiresReport());

        return RoundResponse.from(roundRepository.save(round));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundResponse> listRounds(Long eventId) {
        findEvent(eventId);
        return roundRepository.findByEventIdOrderByOrderNumber(eventId)
                .stream().map(RoundResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundResponse getRound(Long eventId, Long roundId) {
        findEvent(eventId);
        Round round = findRound(roundId, eventId);
        return RoundResponse.from(round);
    }

    @Override
    @Transactional
    public RoundResponse updateRound(Long eventId, Long roundId, UpdateRoundRequest req) {
        Event event = findEvent(eventId);
        validateNotPending(event);
        Round round = findRound(roundId, eventId);

        if (req.name() != null && !req.name().isBlank()) round.setName(req.name());
        if (req.submissionDeadline() != null) round.setSubmissionDeadline(req.submissionDeadline());
        if (req.scoringDeadline() != null) round.setScoringDeadline(req.scoringDeadline());
        if (req.promotionTopN() != null) round.setPromotionTopN(req.promotionTopN());
        if (req.finalRound() != null) round.setIsFinalRound(req.finalRound());
        if (req.requiresRepo() != null) round.setRequiresRepo(req.requiresRepo());
        if (req.requiresDemo() != null) round.setRequiresDemo(req.requiresDemo());
        if (req.requiresSlide() != null) round.setRequiresSlide(req.requiresSlide());
        if (req.requiresReport() != null) round.setRequiresReport(req.requiresReport());

        return RoundResponse.from(roundRepository.save(round));
    }

    // ─── Criteria ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CriteriaSetResponse addCriteriaSet(Long eventId, CreateCriteriaSetRequest req, Long creatorId) {
        Event event = findEvent(eventId);
        validateNotPending(event);
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        // BR-EVT-03: sum of weights must equal 100
        BigDecimal totalWeight = req.criteria().stream()
                .map(AddCriterionRequest::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new BusinessRuleException("BR-EVT-03",
                    "Sum of criterion weights must equal 100, got: " + totalWeight);
        }

        Round round = null;
        if (req.roundId() != null) {
            round = findRound(req.roundId(), eventId);
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: " + req.categoryId()));
            if (!category.getEvent().getId().equals(eventId)) {
                throw new BusinessRuleException("BR-EVT-14",
                        "Category " + req.categoryId() + " does not belong to event " + eventId);
            }
        }

        CriteriaSet cs = new CriteriaSet();
        cs.setName(req.name());
        cs.setDescription(req.description());
        cs.setEvent(event);
        cs.setRound(round);
        cs.setCategory(category);
        cs.setPromotionTopN(req.promotionTopN());
        cs.setIsTemplate(false);
        cs.setIsDefault(false);
        cs.setCreatedBy(creator);
        cs = criteriaSetRepository.save(cs);

        final CriteriaSet savedSet = cs;
        List<ScoringCriterion> criteria = req.criteria().stream().map(c -> {
            ScoringCriterion sc = new ScoringCriterion();
            sc.setCriteriaSet(savedSet);
            sc.setName(c.name());
            sc.setDescription(c.description());
            sc.setMaxScore(c.maxScore());
            sc.setWeight(c.weight());
            sc.setDisplayOrder(c.displayOrder());
            sc.setIsActive(true);
            return scoringCriterionRepository.save(sc);
        }).toList();

        return CriteriaSetResponse.from(cs, criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaSetResponse> listCriteriaSets(Long eventId) {
        findEvent(eventId);
        return criteriaSetRepository.findByEventId(eventId).stream()
                .map(cs -> {
                    List<ScoringCriterion> criteria =
                            scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(cs.getId());
                    return CriteriaSetResponse.from(cs, criteria);
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CriteriaSetResponse getCriteriaSet(Long eventId, Long setId) {
        findEvent(eventId);
        CriteriaSet cs = criteriaSetRepository.findById(setId)
                .filter(s -> s.getEvent() != null && s.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CriteriaSet " + setId + " not found for event " + eventId));
        List<ScoringCriterion> criteria =
                scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(setId);
        return CriteriaSetResponse.from(cs, criteria);
    }

    @Override
    @Transactional
    public CriteriaSetResponse updateCriteriaSet(Long eventId, Long setId, UpdateCriteriaSetRequest req) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        CriteriaSet cs = findCriteriaSet(setId, eventId);

        if (req.name() != null && !req.name().isBlank()) cs.setName(req.name());
        if (req.description() != null) cs.setDescription(req.description());
        cs = criteriaSetRepository.save(cs);

        List<ScoringCriterion> criteria =
                scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(setId);
        return CriteriaSetResponse.from(cs, criteria);
    }

    @Override
    @Transactional
    public CriteriaSetResponse replaceCriteria(Long eventId, Long setId, ReplaceCriteriaRequest req) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        CriteriaSet cs = findCriteriaSet(setId, eventId);

        BigDecimal totalWeight = req.criteria().stream()
                .map(AddCriterionRequest::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
            throw new BusinessRuleException("BR-EVT-03",
                    "Sum of criterion weights must equal 100, got: " + totalWeight);
        }

        List<ScoringCriterion> existing =
                scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(setId);
        scoringCriterionRepository.deleteAll(existing);

        List<ScoringCriterion> newCriteria = req.criteria().stream().map(c -> {
            ScoringCriterion sc = new ScoringCriterion();
            sc.setCriteriaSet(cs);
            sc.setName(c.name());
            sc.setDescription(c.description());
            sc.setMaxScore(c.maxScore());
            sc.setWeight(c.weight());
            sc.setDisplayOrder(c.displayOrder());
            sc.setIsActive(true);
            return scoringCriterionRepository.save(sc);
        }).toList();

        return CriteriaSetResponse.from(cs, newCriteria);
    }

    @Override
    @Transactional
    public void deleteCriteriaSet(Long eventId, Long setId) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        CriteriaSet cs = findCriteriaSet(setId, eventId);

        List<ScoringCriterion> criteria =
                scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(setId);
        scoringCriterionRepository.deleteAll(criteria);
        criteriaSetRepository.delete(cs);
    }

    @Override
    @Transactional
    public CriteriaSetResponse deleteCriterion(Long eventId, Long setId, Long criterionId) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        CriteriaSet cs = findCriteriaSet(setId, eventId);

        ScoringCriterion criterion = scoringCriterionRepository.findById(criterionId)
                .filter(c -> c.getCriteriaSet().getId().equals(setId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Criterion " + criterionId + " not found in criteria set " + setId));
        scoringCriterionRepository.delete(criterion);

        List<ScoringCriterion> remaining =
                scoringCriterionRepository.findByCriteriaSetIdOrderByDisplayOrder(setId);
        return CriteriaSetResponse.from(cs, remaining);
    }

    // ─── Round delete ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteRound(Long eventId, Long roundId) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        findRound(roundId, eventId);

        if (criteriaSetRepository.existsByRoundId(roundId)) {
            throw new BusinessRuleException("BR-EVT-12",
                    "Cannot delete round " + roundId + ": it has attached criteria sets. Remove them first.");
        }
        if (judgeAssignmentRepository.existsByRoundId(roundId)) {
            throw new BusinessRuleException("BR-EVT-12",
                    "Cannot delete round " + roundId + ": it has judge assignments. Revoke them first.");
        }

        roundRepository.deleteById(roundId);
    }

    // ─── Category (FR-EVT-04) ─────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponse addCategory(Long eventId, CreateCategoryRequest req) {
        Event event = findEvent(eventId);
        validateNotPending(event);

        if (categoryRepository.existsByEventIdAndName(eventId, req.name())) {
            throw new BusinessRuleException("BR-EVT-04",
                    "Category name '" + req.name() + "' already exists in this event");
        }

        User mentor = null;
        if (req.mentorId() != null) {
            mentor = userRepository.findById(req.mentorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mentor not found: " + req.mentorId()));
        }

        Category cat = new Category();
        cat.setEvent(event);
        cat.setName(req.name());
        cat.setDescription(req.description());
        cat.setMentor(mentor);
        cat.setIsActive(true);

        return CategoryResponse.from(categoryRepository.save(cat));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long eventId) {
        findEvent(eventId);
        return categoryRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long eventId, Long categoryId) {
        findEvent(eventId);
        return CategoryResponse.from(findCategory(categoryId, eventId));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long eventId, Long categoryId, UpdateCategoryRequest req) {
        Event event = findEvent(eventId);
        validateNotPending(event);
        Category cat = findCategory(categoryId, eventId);

        if (req.name() != null && !req.name().isBlank() && !req.name().equals(cat.getName())) {
            if (categoryRepository.existsByEventIdAndName(eventId, req.name())) {
                throw new BusinessRuleException("BR-EVT-04",
                        "Category name '" + req.name() + "' already exists in this event");
            }
            cat.setName(req.name());
        }
        if (req.description() != null) cat.setDescription(req.description());
        if (req.active() != null) cat.setIsActive(req.active());
        if (req.mentorId() != null) {
            User mentor = userRepository.findById(req.mentorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mentor not found: " + req.mentorId()));
            cat.setMentor(mentor);
        }

        return CategoryResponse.from(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long eventId, Long categoryId) {
        Event event = findEvent(eventId);
        validateConfigEditable(event);
        findCategory(categoryId, eventId);

        if (teamRepository.existsByCategoryId(categoryId)) {
            throw new BusinessRuleException("BR-EVT-13",
                    "Cannot delete category " + categoryId + ": teams are registered to it.");
        }

        categoryRepository.deleteById(categoryId);
    }

    // ─── Judge Assignment ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public JudgeAssignmentResponse assignJudge(Long eventId, Long roundId,
                                               AssignJudgeRequest req, Long assignedById) {
        findEvent(eventId);
        Round round = findRound(roundId, eventId);

        User judge = userRepository.findById(req.judgeId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.judgeId()));

        if (judge.getPrimaryRole() == null || !"JUDGE".equals(judge.getPrimaryRole().getCode())) {
            throw new BusinessRuleException("BR-JDG-01",
                    "User " + req.judgeId() + " does not have the JUDGE role.");
        }

        if (judgeAssignmentRepository.existsByJudgeIdAndRoundIdAndStatus(
                req.judgeId(), roundId, AssignmentStatus.ACTIVE)) {
            throw new BusinessRuleException("BR-JDG-02",
                    "Judge " + req.judgeId() + " is already assigned to round " + roundId + ".");
        }

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedById));

        Category category = null;
        if (req.categoryId() != null) {
            category = findCategory(req.categoryId(), eventId);
        }

        JudgeAssignment ja = new JudgeAssignment();
        ja.setJudge(judge);
        ja.setEvent(round.getEvent());
        ja.setRound(round);
        ja.setCategory(category);
        ja.setAssignedBy(assignedBy);
        ja.setStatus(AssignmentStatus.ACTIVE);

        return JudgeAssignmentResponse.from(judgeAssignmentRepository.save(ja));
    }

    @Override
    @Transactional
    public void revokeJudge(Long eventId, Long roundId, Long assignmentId) {
        findEvent(eventId);
        findRound(roundId, eventId);

        JudgeAssignment ja = judgeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("JudgeAssignment not found: " + assignmentId));

        if (!ja.getRound().getId().equals(roundId)) {
            throw new ForbiddenActionException(
                    "Assignment " + assignmentId + " does not belong to round " + roundId);
        }

        ja.setStatus(AssignmentStatus.REVOKED);
        judgeAssignmentRepository.save(ja);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listJudgeAssignments(Long eventId, Long roundId) {
        findEvent(eventId);
        findRound(roundId, eventId);
        return judgeAssignmentRepository.findByRoundIdAndStatus(roundId, AssignmentStatus.ACTIVE)
                .stream().map(JudgeAssignmentResponse::from).toList();
    }

    // ─── Governance (BR-GOV-02): Super Coordinator approve/reject ────────────

    @Override
    @Transactional
    public EventResponse approveEvent(Long eventId, Long approverId) {
        Event event = findEvent(eventId);

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("BR-GOV-01",
                    "Only PENDING_APPROVAL events can be approved. Current status: " + event.getStatus());
        }

        // BR-GOV-02: separation of duties — approver must differ from owner coordinator
        if (event.getOwnerCoordinator().getId().equals(approverId)) {
            throw new BusinessRuleException("BR-GOV-02",
                    "The owner coordinator cannot approve their own event.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + approverId));

        String oldStatus = event.getStatus().name();
        event.setStatus(EventStatus.APPROVED);
        event.setApprovedBy(approver);
        event.setApprovedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        auditPublisher.log(approver, AuditAction.EVENT_APPROVED, "EVENT", eventId,
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"APPROVED\"}", null, null);

        return EventResponse.from(saved);
    }

    @Override
    @Transactional
    public EventResponse rejectEvent(Long eventId, Long approverId, String reason) {
        Event event = findEvent(eventId);

        if (event.getStatus() != EventStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("BR-GOV-01",
                    "Only PENDING_APPROVAL events can be rejected. Current status: " + event.getStatus());
        }

        // BR-GOV-02: separation of duties
        if (event.getOwnerCoordinator().getId().equals(approverId)) {
            throw new BusinessRuleException("BR-GOV-02",
                    "The owner coordinator cannot reject their own event.");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("BR-GOV-03",
                    "A rejection reason is required.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + approverId));

        String oldStatus = event.getStatus().name();
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        Event saved = eventRepository.save(event);

        auditPublisher.log(approver, AuditAction.EVENT_REJECTED, "EVENT", eventId,
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"REJECTED\"}", reason, null);

        return EventResponse.from(saved);
    }

    // ─── Lifecycle (FR-EVT-07): post-APPROVED transitions ────────────────────

    @Override
    @Transactional
    public EventResponse openEvent(Long eventId, Long coordinatorId) {
        return transitionStatus(eventId, coordinatorId,
                EventStatus.APPROVED, EventStatus.OPEN, AuditAction.EVENT_OPENED);
    }

    @Override
    @Transactional
    public EventResponse startEvent(Long eventId, Long coordinatorId) {
        return transitionStatus(eventId, coordinatorId,
                EventStatus.OPEN, EventStatus.IN_PROGRESS, AuditAction.EVENT_STARTED);
    }

    @Override
    @Transactional
    public EventResponse completeEvent(Long eventId, Long coordinatorId) {
        return transitionStatus(eventId, coordinatorId,
                EventStatus.IN_PROGRESS, EventStatus.COMPLETED, AuditAction.EVENT_COMPLETED);
    }

    @Override
    @Transactional
    public EventResponse archiveEvent(Long eventId, Long coordinatorId) {
        Event event = findEvent(eventId);

        if (!event.getOwnerCoordinator().getId().equals(coordinatorId)) {
            throw new ForbiddenActionException("Only the owner coordinator can archive this event.");
        }
        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new BusinessRuleException("BR-EVT-11",
                    "Cannot archive event: expected COMPLETED but was " + event.getStatus() + ".");
        }

        String oldStatus = event.getStatus().name();
        event.setStatus(EventStatus.ARCHIVED);
        event.setArchivedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        User coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + coordinatorId));
        auditPublisher.log(coordinator, AuditAction.EVENT_ARCHIVED, "EVENT", eventId,
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"ARCHIVED\"}", null, null);

        return EventResponse.from(saved);
    }

    /** Shared transition helper for OPEN / START / COMPLETE. */
    private EventResponse transitionStatus(Long eventId, Long coordinatorId,
                                           EventStatus expectedFrom, EventStatus to,
                                           AuditAction auditAction) {
        Event event = findEvent(eventId);

        if (!event.getOwnerCoordinator().getId().equals(coordinatorId)) {
            throw new ForbiddenActionException(
                    "Only the owner coordinator can change this event's status.");
        }
        if (event.getStatus() != expectedFrom) {
            throw new BusinessRuleException("BR-EVT-11",
                    "Cannot transition to " + to + ": expected " + expectedFrom
                    + " but event is " + event.getStatus() + ".");
        }

        String oldStatus = event.getStatus().name();
        event.setStatus(to);
        Event saved = eventRepository.save(event);

        User coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + coordinatorId));
        auditPublisher.log(coordinator, auditAction, "EVENT", eventId,
                "{\"status\":\"" + oldStatus + "\"}", "{\"status\":\"" + to + "\"}", null, null);

        return EventResponse.from(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateNotPending(Event event) {
        if (event.getStatus() == EventStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("BR-EVT-07",
                    "Event " + event.getId() + " is locked for editing while pending approval.");
        }
    }

    /** Stricter guard for criteria/round/category: only DRAFT and REJECTED allow config edits. */
    private void validateConfigEditable(Event event) {
        EventStatus s = event.getStatus();
        if (s != EventStatus.DRAFT && s != EventStatus.REJECTED) {
            throw new BusinessRuleException("BR-EVT-07",
                    "Event " + event.getId() + " cannot be edited in status " + s
                    + ". Configuration is locked after approval.");
        }
    }

    private CriteriaSet findCriteriaSet(Long setId, Long eventId) {
        return criteriaSetRepository.findById(setId)
                .filter(cs -> cs.getEvent() != null && cs.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CriteriaSet " + setId + " not found for event " + eventId));
    }

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    private Round findRound(Long roundId, Long eventId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
        if (!round.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Round " + roundId + " does not belong to event " + eventId);
        }
        return round;
    }

    private Category findCategory(Long categoryId, Long eventId) {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        if (!cat.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Category " + categoryId + " does not belong to event " + eventId);
        }
        return cat;
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (!eventRepository.existsBySlug(base)) return base;
        return base + "-" + System.currentTimeMillis();
    }
}
