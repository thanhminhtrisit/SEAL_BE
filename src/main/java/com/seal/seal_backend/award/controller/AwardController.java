package com.seal.seal_backend.award.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow: Award management and result publication
 * OWNER: M3 — Nguyễn Công Thiên Ân
 * Only the owner edits files under the 'award' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/awards")
@Tag(name = "Award & Results")
public class AwardController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Award & Results module is alive");
    }
}
