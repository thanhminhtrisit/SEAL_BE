package com.seal.seal_backend.auth.controller;

import com.seal.seal_backend.auth.dto.request.CreateGuestJudgeRequest;
import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.request.RejectAccountRequest;
import com.seal.seal_backend.auth.dto.response.AccountStatusResponse;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.dto.response.GuestJudgeResponse;
import com.seal.seal_backend.auth.dto.response.MeResponse;
import com.seal.seal_backend.auth.dto.response.PendingAccountResponse;
import com.seal.seal_backend.auth.dto.response.RegisterResponse;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.auth.service.AuthService;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.api.PageResponse;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.domain.enums.UserStatus;
import com.seal.seal_backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Flow: Authentication & User Management (FR-AUTH-01..08).
 * OWNER: M1 / Lead — Đồng Thành Minh Trí.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth & Account", description = "Register, login, logout, account approval (FR-AUTH-01..08)")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final NotificationService notificationService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Auth module is alive");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile (FR-AUTH-05)",
               description = "Returns the authenticated user's profile. Requires valid Bearer token.")
    public ApiResponse<MeResponse> me(@CurrentUser UserPrincipal user) {
        return ApiResponse.ok(authService.getCurrentUser(user.getId()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new participant account (FR-AUTH-01/02)",
               description = "Creates account with status PENDING. Admin must approve before login is allowed.")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        Long userId = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created. Awaiting admin approval.",
                        RegisterResponse.pending(userId)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens (FR-AUTH-05)",
               description = "Returns access + refresh tokens. Access token valid for jwt.expiration ms.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (stateless JWT)",
               description = "Server has no token store — client MUST discard both tokens. Returns 200 always.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully.", null));
    }

    // ─── Account approval (FR-AUTH-08) ───────────────────────────────────────

    @GetMapping("/accounts/pending")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @Operation(summary = "List accounts awaiting approval (FR-AUTH-08)",
               description = "Paginated list of PENDING participant accounts for coordinator/admin review.")
    public ApiResponse<PageResponse<PendingAccountResponse>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(authService.listPendingAccounts(PageRequest.of(page, size)));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @Operation(summary = "List accounts by status (FR-AUTH-08)",
               description = "Paginated. Accepts PENDING, ACTIVE, REJECTED, INACTIVE, LOCKED. 'APPROVED' is accepted as an alias for ACTIVE.")
    public ApiResponse<PageResponse<PendingAccountResponse>> listAccountsByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(authService.listAccountsByStatus(
                resolveStatus(status), PageRequest.of(page, size)));
    }

    private UserStatus resolveStatus(String status) {
        String upper = status.toUpperCase();
        if ("APPROVED".equals(upper)) return UserStatus.ACTIVE;
        try {
            return UserStatus.valueOf(upper);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("BR-USR-06", "Unknown account status: " + status);
        }
    }

    @PostMapping("/accounts/{userId}/approve")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @Operation(summary = "Approve a PENDING participant account (FR-AUTH-08, BR-GOV-02)",
               description = "Transitions status PENDING → ACTIVE. Coordinator cannot approve own account.")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> approveAccount(
            @PathVariable Long userId,
            @CurrentUser UserPrincipal user) {
        authService.approveAccount(userId, user.getId());
        notifyBestEffort(userId, "ACCOUNT_APPROVED", "Account Approved",
                "Your account has been approved. You may now log in.");
        return ResponseEntity.ok(ApiResponse.ok("Account approved.",
                new AccountStatusResponse(userId, UserStatus.ACTIVE)));
    }

    @PostMapping("/accounts/{userId}/reject")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @Operation(summary = "Reject a PENDING participant account (FR-AUTH-08)",
               description = "Transitions status PENDING → REJECTED. Reason is mandatory.")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> rejectAccount(
            @PathVariable Long userId,
            @Valid @RequestBody RejectAccountRequest req,
            @CurrentUser UserPrincipal user) {
        authService.rejectAccount(userId, user.getId(), req.reason());
        notifyBestEffort(userId, "ACCOUNT_REJECTED", "Account Rejected",
                "Your account registration was rejected. Reason: " + req.reason());
        return ResponseEntity.ok(ApiResponse.ok("Account rejected.",
                new AccountStatusResponse(userId, UserStatus.REJECTED)));
    }

    // ─── Guest Judge creation (FR-AUTH-09) ───────────────────────────────────

    @PostMapping("/guest-judges")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @Operation(summary = "Create a guest-judge account (FR-AUTH-09)",
               description = "Creates ACTIVE account with role JUDGE. Returns temp password — shown only once.")
    public ResponseEntity<ApiResponse<GuestJudgeResponse>> createGuestJudge(
            @Valid @RequestBody CreateGuestJudgeRequest req,
            @CurrentUser UserPrincipal user) {
        GuestJudgeResponse response = authService.createGuestJudge(req, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Guest judge account created. Share the temporary password securely.", response));
    }

    private void notifyBestEffort(Long recipientId, String type, String title, String message) {
        try {
            notificationService.notifyUser(recipientId, null, type, title, message);
        } catch (Exception e) {
            log.warn("Notification failed for user {} ({}): {}", recipientId, type, e.getMessage());
        }
    }
}
