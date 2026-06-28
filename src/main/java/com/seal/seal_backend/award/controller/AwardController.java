package com.seal.seal_backend.award.controller;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<AwardResponse>> createAward(
            @Valid @RequestBody AwardCreateRequest request,
            @CurrentUser Long userId // Đã gỡ comment và sử dụng token thực tế
    ) {
        AwardResponse response = awardService.createAward(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Xem danh sách giải thưởng của một sự kiện")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AwardResponse>>> getAwardsByEvent(@PathVariable Long eventId) {
        List<AwardResponse> awards = awardService.getAwardsByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(awards));
    }

    @PostMapping("/events/{eventId}/publish")
    @Operation(summary = "Công bố kết quả xếp hạng và giải thưởng cho thí sinh")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<String>> publishResults(
            @PathVariable Long eventId,
            @CurrentUser Long userId
    ) {
        awardService.publishEventResults(eventId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã công bố kết quả thành công!"));
    }

    @GetMapping("/events/{eventId}/eligible-teams")
    @Operation(summary = "Lấy danh sách đội thi từ Vòng Chung Kết để trao giải")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEligibleTeamsForAward(@PathVariable Long eventId) {
        List<Map<String, Object>> teams = awardService.getEligibleTeamsForAward(eventId);
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }
}