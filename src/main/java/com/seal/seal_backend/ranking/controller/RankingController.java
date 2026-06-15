package com.seal.seal_backend.ranking.controller;

import com.seal.seal_backend.common.api.ApiResponse;
// import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/rounds/{roundId}/compute")
    @Operation(summary = "Kích hoạt tính toán điểm và xếp hạng cho một vòng thi")
    // ĐÃ TẮT BẢO VỆ ĐỂ M3 DỄ DÀNG TEST THUẬT TOÁN TÍNH ĐIỂM
    // @PreAuthorize("hasAnyRole('COORDINATOR', 'SUPER_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> computeRanking(
            @PathVariable Long roundId
            // @CurrentUser Long userId
    ) {
        Long fakeUserId = 1L; // Cố định id người dùng để test
        List<RankingResponse> rankings = rankingService.computeRankingForRound(roundId, fakeUserId);
        return ResponseEntity.ok(ApiResponse.ok(rankings));
    }

    @GetMapping("/rounds/{roundId}")
    @Operation(summary = "Xem kết quả xếp hạng của một vòng thi")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> getRankings(@PathVariable Long roundId) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getRankingsByRound(roundId)));
    }
}