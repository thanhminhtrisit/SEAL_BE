package com.seal.seal_backend.ranking.service.impl;

import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    // --- CÁC CLASS RECORD TẠM THỜI ĐỂ CHỨA DỮ LIỆU TỪ MODULE KHÁC ---
    // Sau này bạn sẽ dùng DTO thật từ shared/contract của M1 & M2
    record TeamView(Long id, String name, String status) {}
    record CriterionView(Long id, Double weight) {} // Trọng số: ví dụ 0.4 = 40%
    record ScoreView(Long teamId, Long criterionId, Double scoreValue) {}

    @Override
    public List<RankingResponse> computeRankingForRound(Long roundId, Long userId) {
        log.info("Bắt đầu thuật toán tính toán xếp hạng cho round: {}", roundId);

        // ====================================================================================
        // PHẦN 1: LẤY DỮ LIỆU (Đang dùng Mock Data, sau này thay bằng gọi qua Port)
        // ====================================================================================

        // 1. Lấy danh sách Team (Giả sử lấy từ TeamQueryPort)
        List<TeamView> allTeams = List.of(
                new TeamView(101L, "Team Alpha", "ACTIVE"),
                new TeamView(102L, "Team Beta", "ACTIVE"),
                new TeamView(103L, "Team Gamma", "DISQUALIFIED") // Team này sẽ bị loại theo BR-RNK-04
        );

        // 2. Lấy tiêu chí chấm điểm và trọng số của vòng này (Giả sử lấy từ EventQueryPort)
        List<CriterionView> criteria = List.of(
                new CriterionView(1L, 0.4), // Tiêu chí 1: 40% số điểm
                new CriterionView(2L, 0.6)  // Tiêu chí 2: 60% số điểm
        );

        // 3. Lấy tất cả điểm giám khảo đã chấm (Giả sử lấy từ ScoringQueryPort)
        List<ScoreView> allScores = List.of(
                // Điểm của Team Alpha (2 giám khảo chấm)
                new ScoreView(101L, 1L, 90.0), new ScoreView(101L, 2L, 85.0), // Giám khảo A
                new ScoreView(101L, 1L, 80.0), new ScoreView(101L, 2L, 95.0), // Giám khảo B

                // Điểm của Team Beta (2 giám khảo chấm)
                new ScoreView(102L, 1L, 70.0), new ScoreView(102L, 2L, 75.0), // Giám khảo A
                new ScoreView(102L, 1L, 80.0), new ScoreView(102L, 2L, 85.0)  // Giám khảo B
        );

        int promotionTopN = 1; // Giả sử quy tắc (BR-RNK-05) là chỉ Top 1 được thăng hạng

        // ====================================================================================
        // PHẦN 2: THUẬT TOÁN TÍNH ĐIỂM BẰNG JAVA STREAM (LOGIC THẬT)
        // ====================================================================================

        // BƯỚC 1: Lọc team theo BR-RNK-04 (Loại bỏ DISQUALIFIED)
        List<TeamView> validTeams = allTeams.stream()
                .filter(team -> !"DISQUALIFIED".equals(team.status()))
                .toList();

        // BƯỚC 2: Nhóm toàn bộ điểm số theo từng TeamId
        Map<Long, List<ScoreView>> scoresGroupedByTeam = allScores.stream()
                .collect(Collectors.groupingBy(ScoreView::teamId));

        List<RankingResponse> rankings = new ArrayList<>();

        // BƯỚC 3: Tính điểm cho từng Team hợp lệ
        for (TeamView team : validTeams) {
            List<ScoreView> teamScores = scoresGroupedByTeam.getOrDefault(team.id(), List.of());

            // Nhóm điểm của team này theo Từng Tiêu Chí (CriterionId) và TÍNH ĐIỂM TRUNG BÌNH của tiêu chí đó
            Map<Long, Double> avgScoreByCriterion = teamScores.stream()
                    .collect(Collectors.groupingBy(
                            ScoreView::criterionId,
                            Collectors.averagingDouble(ScoreView::scoreValue) // Tính trung bình cộng
                    ));

            // Áp dụng BR-RNK-02: Final = SUM(Average * Weight)
            double finalScore = 0.0;
            for (CriterionView criterion : criteria) {
                double avgScore = avgScoreByCriterion.getOrDefault(criterion.id(), 0.0);
                finalScore += avgScore * criterion.weight();
            }

            // Tạo đối tượng RankingResponse (Tạm thời rank = 0, isPromoted = false)
            rankings.add(new RankingResponse(
                    null, team.id(), team.name(), roundId,
                    Math.round(finalScore * 100.0) / 100.0, // Làm tròn 2 chữ số thập phân
                    0, false
            ));
        }

        // BƯỚC 4: Sắp xếp danh sách giảm dần theo điểm tổng
        rankings.sort(Comparator.comparing(RankingResponse::totalScore).reversed());

        // BƯỚC 5: Gán thứ hạng (Rank) và xét thăng hạng (Promotion) theo BR-RNK-05
        List<RankingResponse> finalRankings = new ArrayList<>();
        int currentRank = 1;

        for (RankingResponse r : rankings) {
            boolean isPromoted = currentRank <= promotionTopN;
            finalRankings.add(new RankingResponse(
                    r.rankingId(), r.teamId(), r.teamName(), r.roundId(),
                    r.totalScore(), currentRank, isPromoted
            ));
            currentRank++;
        }

        // BƯỚC 6: Xóa Ranking cũ và Lưu danh sách finalRankings vào Database
        // rankingRepository.deleteByRoundId(roundId);
        // ... code lưu DB ở đây

        return finalRankings;
    }

    @Override
    public List<RankingResponse> getRankingsByRound(Long roundId) {
        return List.of();
    }
}