package com.seal.seal_backend.event.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow: Event and round configuration
 * OWNER: M1 — Đồng Thành Minh Trí
 * Only the owner edits files under the 'event' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Event & Round Configuration")
public class EventController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Event & Round Configuration module is alive");
    }
}
