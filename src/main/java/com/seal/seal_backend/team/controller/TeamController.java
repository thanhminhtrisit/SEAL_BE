package com.seal.seal_backend.team.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flow: Team registration and account approval
 * OWNER: M1 — Đồng Thành Minh Trí
 * Only the owner edits files under the 'team' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/teams")
@Tag(name = "Team & Account Approval")
public class TeamController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Team & Account Approval module is alive");
    }
}
