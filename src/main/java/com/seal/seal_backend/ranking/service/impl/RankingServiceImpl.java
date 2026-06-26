package com.seal.seal_backend.ranking.service.impl;

import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.repository.RankingRepository;
import com.seal.seal_backend.domain.repository.RoundRepository; // Đảm bảo đã import RoundRepository
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingRepository rankingRepository;
    private final RoundRepository roundRepository; // Cần thiết để findById
    private final RankingDataProvider dataProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public List<RankingResponse> computeRankingForRound(Long roundId, Long userId) {
        log.info("Bắt đầu thuật toán tính toán xếp hạng cho round: {}", roundId);

        // 1. LẤY DỮ LIỆU TỪ MYSQL
        List<RankingDataProvider.TeamView> allTeams = dataProvider.getTeamsInRound(roundId);
        List<RankingDataProvider.CriterionView> criteria = dataProvider.getCriteriaForRound(roundId);
        List<RankingDataProvider.ScoreView> allScores = dataProvider.getScoresForRound(roundId);

        Round currentRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi với ID: " + roundId));

        Integer promotionTopN = currentRound.getPromotionTopN();

        // ---> BƯỚC CHUẨN BỊ TIE-BREAKER: Tìm tiêu chí quan trọng nhất (Trọng số cao nhất)
        Long priorityCriterionId = criteria.stream()
                .max(Comparator.comparing(RankingDataProvider.CriterionView::weight))
                .map(RankingDataProvider.CriterionView::id)
                .orElse(null);

        // 2. THUẬT TOÁN TÍNH ĐIỂM
        List<RankingDataProvider.TeamView> validTeams = allTeams.stream()
                .filter(team -> !"DISQUALIFIED".equals(team.status()))
                .toList();

        Map<Long, List<RankingDataProvider.ScoreView>> scoresGroupedByTeam = allScores.stream()
                .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::teamId));

        List<RankingResponse> preliminaryRankings = new ArrayList<>();

        // Map dùng để nhớ điểm số của tiêu chí phụ để mang ra so sánh khi bằng điểm
        Map<Long, Double> priorityScoresByTeam = new HashMap<>();

        for (RankingDataProvider.TeamView team : validTeams) {
            List<RankingDataProvider.ScoreView> teamScores = scoresGroupedByTeam.getOrDefault(team.id(), List.of());

            Map<Long, Double> avgScoreByCriterion = teamScores.stream()
                    .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::criterionId,
                            Collectors.averagingDouble(RankingDataProvider.ScoreView::scoreValue)));

            double finalScore = 0.0;
            double priorityScore = 0.0; // Điểm của tiêu chí quan trọng nhất

            for (RankingDataProvider.CriterionView criterion : criteria) {
                double avgScore = avgScoreByCriterion.getOrDefault(criterion.id(), 0.0);
                double actualWeight = criterion.weight() / 100.0;
                finalScore += avgScore * actualWeight;

                // Lấy điểm của tiêu chí ưu tiên cất đi
                if (priorityCriterionId != null && criterion.id().equals(priorityCriterionId)) {
                    priorityScore = avgScore;
                }
            }

            priorityScoresByTeam.put(team.id(), priorityScore);

            preliminaryRankings.add(new RankingResponse(
                    null, team.id(), team.name(), "Chung", roundId,
                    Math.round(finalScore * 1000.0) / 1000.0, 0, false));
        }

        // ---> NÂNG CẤP TIE-BREAKER LOGIC (SẮP XẾP ĐA ĐIỀU KIỆN) <---
        preliminaryRankings.sort((r1, r2) -> {
            // So sánh 1: Xét Tổng điểm
            int scoreCompare = Double.compare(r2.totalScore(), r1.totalScore());
            if (scoreCompare != 0) {
                return scoreCompare; // Khác điểm thì phân định luôn
            }

            // So sánh 2: Bằng điểm tổng -> Kéo điểm tiêu chí ưu tiên ra phân định
            Double p1 = priorityScoresByTeam.getOrDefault(r1.teamId(), 0.0);
            Double p2 = priorityScoresByTeam.getOrDefault(r2.teamId(), 0.0);
            return Double.compare(p2, p1);
        });

        // 3. XÓA CŨ & LƯU MỚI
        log.info("Xóa bảng xếp hạng cũ của vòng thi: {}", roundId);
        rankingRepository.deleteByRoundId(roundId);

        // ... (Giữ nguyên đoạn code xử lý lưu DB và Map DTO từ vòng lặp entitiesToSave trở đi)

        Map<Long, Long> teamCategoryMap = new HashMap<>();
        for (RankingDataProvider.TeamView t : validTeams) {
            if (t.categoryId() != null) {
                teamCategoryMap.put(t.id(), t.categoryId());
            }
        }

        List<Ranking> entitiesToSave = new ArrayList<>();
        int currentRank = 1;

        for (RankingResponse r : preliminaryRankings) {

            // LOGIC THĂNG HẠNG: An toàn xử lý null cho Vòng chung kết
            boolean isPromoted = (promotionTopN != null && currentRank <= promotionTopN);

            // Tạo các Dummy Object chỉ chứa ID để JPA tự map Foreign Key
            Event eventRef = new Event(); eventRef.setId(currentRound.getEvent().getId()); // Tự động map ID Event thật
            Round roundRef = new Round(); roundRef.setId(roundId);
            Team teamRef = new Team(); teamRef.setId(r.teamId());

            // Xử lý an toàn: Nếu Auth truyền null, bỏ qua không set User để tránh crash Hibernate
            User userRef = null;
            if (userId != null) {
                userRef = new User();
                userRef.setId(userId);
            }
            Category categoryRef = null;
            Long actualCategoryId = teamCategoryMap.get(r.teamId());
            if (actualCategoryId != null) {
                categoryRef = new Category();
                categoryRef.setId(actualCategoryId);
            }

            // Dùng Setter chuẩn của Entity
            Ranking entity = new Ranking();
            entity.setEvent(eventRef);
            entity.setRound(roundRef);
            entity.setCategory(categoryRef);
            entity.setTeam(teamRef);

            // Ép kiểu Double về BigDecimal
            entity.setTotalScore(BigDecimal.valueOf(r.totalScore()));
            entity.setRankPosition(currentRank);
            entity.setIsPromoted(isPromoted);
            entity.setComputedBy(userRef);
            entity.setSnapshotNote("Computed from MySQL real data");

            entitiesToSave.add(entity);
            currentRank++;
        }

        List<Ranking> savedEntities = rankingRepository.saveAll(entitiesToSave);

        // 4. MAP NGƯỢC RA DTO
        Map<Long, String> teamNameMap = validTeams.stream()
                .collect(Collectors.toMap(RankingDataProvider.TeamView::id, RankingDataProvider.TeamView::name));

        Map<Long, String> teamCategoryNameMap = validTeams.stream()
                .collect(Collectors.toMap(
                        RankingDataProvider.TeamView::id,
                        t -> t.categoryName() != null ? t.categoryName() : "Chung"
                ));

        return savedEntities.stream()
                .map(e -> new RankingResponse(
                        e.getId(),
                        e.getTeam().getId(),
                        teamNameMap.getOrDefault(e.getTeam().getId(), "Unknown"),
                        teamCategoryNameMap.getOrDefault(e.getTeam().getId(), "Chung"),
                        e.getRound().getId(),
                        e.getTotalScore().doubleValue(),
                        e.getRankPosition(),
                        e.getIsPromoted()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getRankingsByRound(Long roundId) {
        return rankingRepository.findByRoundIdOrderByRankPositionAsc(roundId)
                .stream()
                .map(e -> new RankingResponse(
                        e.getId(),
                        e.getTeam().getId(),
                        e.getTeam().getName(),
                        e.getCategory() != null ? e.getCategory().getName() : "Chung",
                        e.getRound().getId(),
                        e.getTotalScore().doubleValue(),
                        e.getRankPosition(),
                        e.getIsPromoted()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void disqualifyTeam(Long teamId, String reason, Long userId) {
        log.info("Coordinator {} đang đình chỉ Team {} với lý do: {}", userId, teamId, reason);

        // 1. Cập nhật trạng thái đội thành DISQUALIFIED
        String updateSql = "UPDATE teams SET status = 'DISQUALIFIED' WHERE id = ?";
        jdbcTemplate.update(updateSql, teamId);
    }
}