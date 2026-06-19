package com.seal.seal_backend.event.service;

import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import java.util.List;

public interface EventService {

    // --- Event ---
    EventResponse create(CreateEventRequest req, Long creatorId);
    EventResponse getById(Long id);
    List<EventSummaryResponse> listAll();
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

    // --- Category (FR-EVT-04) ---
    CategoryResponse addCategory(Long eventId, CreateCategoryRequest req);
    List<CategoryResponse> listCategories(Long eventId);
    CategoryResponse getCategory(Long eventId, Long categoryId);
    CategoryResponse updateCategory(Long eventId, Long categoryId, UpdateCategoryRequest req);
}
