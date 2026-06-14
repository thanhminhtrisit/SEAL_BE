package com.seal.seal_backend.auth.controller;

import com.seal.seal_backend.auth.dto.request.LoginRequest;
import com.seal.seal_backend.auth.dto.request.RegisterRequest;
import com.seal.seal_backend.auth.dto.response.AuthResponse;
import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Flow: Authentication & User Management (FR-AUTH-01..06) + participant account approval.
 * OWNER: M1 / Lead — Đồng Thành Minh Trí.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth & Account")
public class AuthController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() { return ApiResponse.ok("Auth module is alive"); }

    // TODO(M1): implement
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("stub: register " + req.email());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(new AuthResponse("stub-access-token", "stub-refresh-token"));
    }
}
