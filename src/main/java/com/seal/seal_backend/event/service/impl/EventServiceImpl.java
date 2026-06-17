package com.seal.seal_backend.event.service.impl;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import com.seal.seal_backend.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public List<EventSummaryResponse> listAll() {
        return eventRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(EventSummaryResponse::from).toList();
    }

    @Override
    @Transactional
    public EventResponse update(Long id, UpdateEventRequest req) {
        Event event = findEvent(id);

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

    // ─── Round ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoundResponse addRound(Long eventId, CreateRoundRequest req) {
        Event event = findEvent(eventId);

        if (roundRepository.existsByEventIdAndOrderNumber(eventId, req.orderNumber())) {
            throw new BusinessRuleException("BR-EVT-02",
                    "A round with order_number " + req.orderNumber() + " already exists in this event");
        }

        Round round = new Round();
        round.setEvent(event);
        round.setName(req.name());
        round.setOrderNumber(req.orderNumber());
        round.setSubmissionDeadline(req.submissionDeadline());
        round.setPromotionTopN(req.promotionTopN());
        round.setIsFinalRound(req.finalRound());
        round.setStatus(RoundStatus.DRAFT);

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
        findEvent(eventId);
        Round round = findRound(roundId, eventId);

        if (req.name() != null && !req.name().isBlank()) round.setName(req.name());
        if (req.submissionDeadline() != null) round.setSubmissionDeadline(req.submissionDeadline());
        if (req.promotionTopN() != null) round.setPromotionTopN(req.promotionTopN());
        if (req.finalRound() != null) round.setIsFinalRound(req.finalRound());

        return RoundResponse.from(roundRepository.save(round));
    }

    // ─── Criteria ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CriteriaSetResponse addCriteriaSet(Long eventId, CreateCriteriaSetRequest req, Long creatorId) {
        Event event = findEvent(eventId);
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

        CriteriaSet cs = new CriteriaSet();
        cs.setName(req.name());
        cs.setDescription(req.description());
        cs.setEvent(event);
        cs.setRound(round);
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

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

    private String uniqueSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (!eventRepository.existsBySlug(base)) return base;
        String candidate = base + "-" + System.currentTimeMillis();
        return candidate;
    }
}
