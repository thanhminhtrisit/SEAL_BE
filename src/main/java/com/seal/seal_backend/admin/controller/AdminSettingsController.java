package com.seal.seal_backend.admin.controller;

import com.seal.seal_backend.admin.dto.AutoApproveRequest;
import com.seal.seal_backend.admin.dto.AutoApproveResponse;
import com.seal.seal_backend.admin.service.AdminSettingsService;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** FR-ADM-02 — Admin platform settings (auto-approve accounts toggle). */
@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "Admin — Settings", description = "Admin-only platform settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping("/auto-approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get AUTO_APPROVE_ACCOUNTS state")
    public ApiResponse<AutoApproveResponse> getAutoApprove() {
        return ApiResponse.ok(new AutoApproveResponse(adminSettingsService.isAutoApproveEnabled()));
    }

    @PutMapping("/auto-approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable/disable auto-approval of newly registered accounts")
    public ApiResponse<AutoApproveResponse> setAutoApprove(
            @RequestBody AutoApproveRequest req, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(new AutoApproveResponse(
                adminSettingsService.setAutoApprove(req.enabled(), user.getId())));
    }
}
