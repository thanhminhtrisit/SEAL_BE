package com.seal.seal_backend.ranking.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.domain.repository.RoundRepository;
import com.seal.seal_backend.domain.repository.SubmissionRepository;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
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
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;


    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("Ranking module is running!"));
    }

    @PostMapping("/rounds/{roundId}/compute")
    @Operation(summary = "Kích hoạt tính toán điểm và xếp hạng cho một vòng thi")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> computeRanking(
            @PathVariable Long roundId,
            @CurrentUser Long userId
    ) {
        List<RankingResponse> rankings = rankingService.computeRankingForRound(roundId, userId);
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
            @CurrentUser Long userId
    ) {
        String reason = requestBody.get("reason");
        rankingService.disqualifyTeam(teamId, reason, userId);

        return ResponseEntity.ok(ApiResponse.ok("Đã đình chỉ đội thi thành công"));
    }

    @GetMapping("/events/{eventId}/disqualified")
    @Operation(summary = "Lấy danh sách các đội bị đình chỉ trong sự kiện")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDisqualifiedTeams(@PathVariable Long eventId) {
        List<Map<String, Object>> disqualifiedTeams = rankingService.getDisqualifiedTeams(eventId);
        return ResponseEntity.ok(ApiResponse.ok(disqualifiedTeams));
    }

    @PostMapping("/rounds/{roundId}/promote")
    @Operation(summary = "Thăng hạng thủ công các đội sang vòng tiếp theo")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<String>> promoteTeams(
            @PathVariable Long roundId,
            @RequestBody List<Long> teamIds) {

        // Chỉ cần gọi service, nếu lỗi nó sẽ tự quăng RuntimeException
        // Global Exception Handler của hệ thống sẽ tự lo phần trả về lỗi
        rankingService.promoteTeamsToNextRound(roundId, teamIds);

        return ResponseEntity.ok(ApiResponse.ok("Đã chuyển các đội vào vòng tiếp theo thành công!"));
    }
}