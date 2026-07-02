package com.seal.seal_backend.event.service;

import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import java.util.List;

public interface EventService {

    // --- Event ---
    EventResponse create(CreateEventRequest req, Long creatorId);
    EventResponse getById(Long id);
    List<EventSummaryResponse> listAll(EventStatus status);
    EventResponse update(Long id, UpdateEventRequest req);

    // --- Round ---
    RoundResponse addRound(Long eventId, CreateRoundRequest req);
    List<RoundResponse> listRounds(Long eventId);
    RoundResponse getRound(Long eventId, Long roundId);
    RoundResponse updateRound(Long eventId, Long roundId, UpdateRoundRequest req);

    // --- Criteria ---
    CriteriaSetResponse addCriteriaSet(Long eventId, CreateCriteriaSetRequest req, Long creatorId);
    List<CriteriaSetResponse> listCriteriaSets(Long eventId);
    CriteriaSetResponse getCriteriaSet(Long eventId, Long setId);
    CriteriaSetResponse updateCriteriaSet(Long eventId, Long setId, UpdateCriteriaSetRequest req);
    CriteriaSetResponse replaceCriteria(Long eventId, Long setId, ReplaceCriteriaRequest req);
    void deleteCriteriaSet(Long eventId, Long setId);
    CriteriaSetResponse deleteCriterion(Long eventId, Long setId, Long criterionId);

    // --- Round delete ---
    void deleteRound(Long eventId, Long roundId);

    // --- Category (FR-EVT-04) ---
    CategoryResponse addCategory(Long eventId, CreateCategoryRequest req);
    List<CategoryResponse> listCategories(Long eventId);
    CategoryResponse getCategory(Long eventId, Long categoryId);
    CategoryResponse updateCategory(Long eventId, Long categoryId, UpdateCategoryRequest req);
    void deleteCategory(Long eventId, Long categoryId);

    // --- Judge Assignment ---
    JudgeAssignmentResponse assignJudge(Long eventId, Long roundId, AssignJudgeRequest req, Long assignedById);
    void revokeJudge(Long eventId, Long roundId, Long assignmentId);
    List<JudgeAssignmentResponse> listJudgeAssignments(Long eventId, Long roundId);

    // --- Submit (FR-EVT-07) ---
    EventResponse submitEvent(Long eventId, Long coordinatorId);

    // --- Governance (BR-GOV-02): Super Coordinator approve/reject ---
    EventResponse approveEvent(Long eventId, Long approverId);
    EventResponse rejectEvent(Long eventId, Long approverId, String reason);

    // --- Lifecycle (FR-EVT-07): APPROVED → OPEN → IN_PROGRESS → COMPLETED → ARCHIVED ---
    EventResponse openEvent(Long eventId, Long coordinatorId);
    EventResponse startEvent(Long eventId, Long coordinatorId);
    EventResponse completeEvent(Long eventId, Long coordinatorId);
    EventResponse archiveEvent(Long eventId, Long coordinatorId);
}
