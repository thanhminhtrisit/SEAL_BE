package com.seal.seal_backend.scoring.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow: Judge scoring and evaluation
 * OWNER: M2 — Lê Quang Hải
 * Only the owner edits files under the 'scoring' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/scoring")
@Tag(name = "Scoring & Evaluation")
public class ScoringController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Scoring & Evaluation module is alive");
    }
}
