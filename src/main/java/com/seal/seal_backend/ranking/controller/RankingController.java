package com.seal.seal_backend.ranking.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.ranking.dto.response.CategoryResponse;
import com.seal.seal_backend.ranking.dto.response.DisqualifiedTeamResponse;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.dto.response.ScoreBreakdownResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rankings")
@Tag(name = "Ranking & Promotion", description = "Quản lý tính toán điểm, xếp hạng và thăng hạng")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;


    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("Ranking module is running!"));
    }

    @GetMapping("/events/{eventId}/categories")
    @Operation(summary = "Lấy danh sách các hạng mục (Category) của sự kiện")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(@PathVariable Long eventId) {
        List<CategoryResponse> categories = rankingService.getCategoriesByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping("/rounds/{roundId}/compute")
    @Operation(summary = "Kích hoạt tính toán điểm và xếp hạng (hỗ trợ lọc theo Hạng mục/Category)")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> computeRanking(
            @PathVariable Long roundId,
            @RequestParam(required = false, defaultValue = "0") Long categoryId, // Thêm dòng này
            @CurrentUser UserPrincipal user
    ) {
        List<RankingResponse> rankings = rankingService.computeRankingForRound(roundId, categoryId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(rankings));
    }

    @GetMapping("/rounds/{roundId}")
    @Operation(summary = "Xem kết quả xếp hạng của một vòng thi")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> getRankings(@PathVariable Long roundId) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getRankingsByRound(roundId)));
    }

    @PostMapping("/teams/{teamId}/disqualify")
    @Operation(summary = "Đình chỉ đội thi do vi phạm (FR-RNK-04)")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<String>> disqualifyTeam(
            @PathVariable Long teamId,
            @RequestBody Map<String, String> requestBody,
            @CurrentUser UserPrincipal user
    ) {
        String reason = requestBody.get("reason");
        rankingService.disqualifyTeam(teamId, reason, user.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã đình chỉ đội thi thành công"));
    }

    @GetMapping("/events/{eventId}/disqualified")
    @Operation(summary = "Lấy danh sách các đội bị đình chỉ trong sự kiện")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<DisqualifiedTeamResponse>>> getDisqualifiedTeams(@PathVariable Long eventId) {
        List<DisqualifiedTeamResponse> disqualifiedTeams = rankingService.getDisqualifiedTeams(eventId);
        return ResponseEntity.ok(ApiResponse.ok(disqualifiedTeams));
    }

    @PostMapping("/rounds/{roundId}/promote")
    @Operation(summary = "Thăng hạng thủ công các đội sang vòng tiếp theo")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<String>> promoteTeams(
            @PathVariable Long roundId,
            @RequestBody List<Long> teamIds,
            @CurrentUser UserPrincipal user) {

        rankingService.promoteTeamsToNextRound(roundId, teamIds, user.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã chuyển các đội vào vòng tiếp theo thành công!"));
    }

    @GetMapping("/teams/{teamId}/rounds/{roundId}/breakdown")
    @Operation(summary = "Xem chi tiết bảng điểm (Score Breakdown) của một đội thi trong một vòng")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScoreBreakdownResponse>>> getScoreBreakdown(
            @PathVariable Long teamId,
            @PathVariable Long roundId
    ) {
        List<ScoreBreakdownResponse> breakdown = rankingService.getScoreBreakdown(teamId, roundId);
        return ResponseEntity.ok(ApiResponse.ok(breakdown));
    }
}