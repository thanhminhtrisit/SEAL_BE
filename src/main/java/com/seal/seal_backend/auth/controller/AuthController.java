package com.seal.seal_backend.auth.controller;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.auth.dto.response.RegisterResponse;
import com.seal.seal_backend.auth.service.AuthService;
import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Flow: Authentication & User Management (FR-AUTH-01..06).
 * OWNER: M1 / Lead — Đồng Thành Minh Trí.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth & Account", description = "Register, login, logout (FR-AUTH-01..06)")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Auth module is alive");
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
}
