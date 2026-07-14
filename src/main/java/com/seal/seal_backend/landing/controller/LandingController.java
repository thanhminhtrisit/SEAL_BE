package com.seal.seal_backend.landing.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.landing.dto.LandingResponse;
import com.seal.seal_backend.landing.service.LandingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PUBLIC (no auth) endpoints that back the marketing/landing page. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class LandingController {

    private final LandingService landingService;

    @GetMapping("/landing")
    @Operation(summary = "Public landing data: stats + events open for registration (no auth)")
    public ApiResponse<LandingResponse> landing() {
        return ApiResponse.ok(landingService.getLanding());
    }
}
