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
import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.common.api.ApiResponse;
// import com.seal.seal_backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/awards")
@Tag(name = "Award & Results", description = "Quản lý Giải thưởng và Công bố Kết quả")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("Award module is running!"));
    }

    @PostMapping
    @Operation(summary = "Gán giải thưởng cho một đội thi")
    // @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<AwardResponse>> createAward(
            @Valid @RequestBody AwardCreateRequest request
            // @CurrentUser Long userId
    ) {
        Long fakeUserId = 1L; // Cố định id người dùng để test (Admin)
        AwardResponse response = awardService.createAward(request, fakeUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Xem danh sách giải thưởng của một sự kiện")
    public ResponseEntity<ApiResponse<List<AwardResponse>>> getAwardsByEvent(@PathVariable Long eventId) {
        List<AwardResponse> awards = awardService.getAwardsByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(awards));
    }
}
