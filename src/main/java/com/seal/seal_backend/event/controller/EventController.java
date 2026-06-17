package com.seal.seal_backend.event.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.event.dto.request.*;
import com.seal.seal_backend.event.dto.response.*;
import com.seal.seal_backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Event & Round Configuration", description = "FR-EVT-01/02/03 — event, round, criteria management")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Event & Round Configuration module is alive");
    }

    // ─── Event ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create event (FR-EVT-01)")
    public ResponseEntity<ApiResponse<EventResponse>> create(
            @Valid @RequestBody CreateEventRequest req,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.create(req, user.getId())));
    }

    @GetMapping
    @Operation(summary = "List all events")
    public ApiResponse<List<EventSummaryResponse>> listAll() {
        return ApiResponse.ok(eventService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by id")
    public ApiResponse<EventResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update event (name, description, registration window)")
    public ApiResponse<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest req) {
        return ApiResponse.ok(eventService.update(id, req));
    }

    // ─── Round ────────────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/rounds")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Add round to event (FR-EVT-02)")
    public ResponseEntity<ApiResponse<RoundResponse>> addRound(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateRoundRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.addRound(eventId, req)));
    }

    @GetMapping("/{eventId}/rounds")
    @Operation(summary = "List rounds for event")
    public ApiResponse<List<RoundResponse>> listRounds(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.listRounds(eventId));
    }

    @GetMapping("/{eventId}/rounds/{roundId}")
    @Operation(summary = "Get round by id")
    public ApiResponse<RoundResponse> getRound(
            @PathVariable Long eventId, @PathVariable Long roundId) {
        return ApiResponse.ok(eventService.getRound(eventId, roundId));
    }

    @PatchMapping("/{eventId}/rounds/{roundId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update round")
    public ApiResponse<RoundResponse> updateRound(
            @PathVariable Long eventId,
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateRoundRequest req) {
        return ApiResponse.ok(eventService.updateRound(eventId, roundId, req));
    }

    // ─── Criteria Sets ────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/criteria-sets")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create criteria set with criteria (BR-EVT-03: weights must sum to 100)")
    public ResponseEntity<ApiResponse<CriteriaSetResponse>> addCriteriaSet(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateCriteriaSetRequest req,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.addCriteriaSet(eventId, req, user.getId())));
    }

    @GetMapping("/{eventId}/criteria-sets")
    @Operation(summary = "List criteria sets for event")
    public ApiResponse<List<CriteriaSetResponse>> listCriteriaSets(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.listCriteriaSets(eventId));
    }

    @GetMapping("/{eventId}/criteria-sets/{setId}")
    @Operation(summary = "Get criteria set by id")
    public ApiResponse<CriteriaSetResponse> getCriteriaSet(
            @PathVariable Long eventId, @PathVariable Long setId) {
        return ApiResponse.ok(eventService.getCriteriaSet(eventId, setId));
    }
}
