package com.seal.seal_backend.auth.controller;

import com.seal.seal_backend.auth.dto.response.JudgeResponse;
import com.seal.seal_backend.auth.dto.response.MentorResponse;
import com.seal.seal_backend.auth.service.UserDirectoryService;
import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "User Directory", description = "Judge & mentor directory for event configuration")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserDirectoryService userDirectoryService;

    @GetMapping("/api/judges")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List active judges (internal + guest) for round assignment")
    public ApiResponse<List<JudgeResponse>> listJudges() {
        return ApiResponse.ok(userDirectoryService.listJudges());
    }

    @GetMapping("/api/mentors")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "List active mentors")
    public ApiResponse<List<MentorResponse>> listMentors() {
        return ApiResponse.ok(userDirectoryService.listMentors());
    }
}
