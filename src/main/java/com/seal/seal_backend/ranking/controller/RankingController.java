package com.seal.seal_backend.ranking.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow: Automated ranking and promotion logic
 * OWNER: M3 — Nguyễn Công Thiên Ân
 * Only the owner edits files under the 'ranking' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/rankings")
@Tag(name = "Ranking & Promotion")
public class RankingController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Ranking & Promotion module is alive");
    }
}
