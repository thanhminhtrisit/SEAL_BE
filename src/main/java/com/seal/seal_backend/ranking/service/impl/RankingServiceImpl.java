package com.seal.seal_backend.ranking.service.impl;

import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.repository.RankingRepository;
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

        int promotionTopN = 10;

        // 2. THUẬT TOÁN TÍNH ĐIỂM
        List<RankingDataProvider.TeamView> validTeams = allTeams.stream()
                .filter(team -> !"DISQUALIFIED".equals(team.status()))
                .toList();

        Map<Long, List<RankingDataProvider.ScoreView>> scoresGroupedByTeam = allScores.stream()
                .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::teamId));

        List<RankingResponse> preliminaryRankings = new ArrayList<>();

        for (RankingDataProvider.TeamView team : validTeams) {
            List<RankingDataProvider.ScoreView> teamScores = scoresGroupedByTeam.getOrDefault(team.id(), List.of());

            Map<Long, Double> avgScoreByCriterion = teamScores.stream()
                    .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::criterionId,
                            Collectors.averagingDouble(RankingDataProvider.ScoreView::scoreValue)));

            double finalScore = 0.0;
            for (RankingDataProvider.CriterionView criterion : criteria) {
                double actualWeight = criterion.weight() / 100.0;
                finalScore += avgScoreByCriterion.getOrDefault(criterion.id(), 0.0) * actualWeight;
            }

            preliminaryRankings.add(new RankingResponse(
                    null, team.id(), team.name(), "Chung", roundId,
                    Math.round(finalScore * 1000.0) / 1000.0, 0, false));
        }

        preliminaryRankings.sort(Comparator.comparing(RankingResponse::totalScore).reversed());

        // 3. XÓA CŨ & LƯU MỚI (Dùng Setter và Proxy Object)
        log.info("Xóa bảng xếp hạng cũ của vòng thi: {}", roundId);
        rankingRepository.deleteByRoundId(roundId);

        Map<Long, Long> teamCategoryMap = new HashMap<>();
        for (RankingDataProvider.TeamView t : validTeams) {
            if (t.categoryId() != null) {
                teamCategoryMap.put(t.id(), t.categoryId());
            }
        }

        List<Ranking> entitiesToSave = new ArrayList<>();
        int currentRank = 1;

        for (RankingResponse r : preliminaryRankings) {
            boolean isPromoted = currentRank <= promotionTopN;

            // Tạo các Dummy Object chỉ chứa ID để JPA tự map Foreign Key
            Event eventRef = new Event(); eventRef.setId(1L);
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

        // ---> THÊM MỚI: Tạo thêm 1 Map để lưu tên Category của từng Team
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

                        // ---> SỬA Ở ĐÂY: Lấy tên Category từ Map, thay vì gọi e.getCategory().getName()
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
                        e.getTeam().getName(), // Sửa dòng này: Lấy tên thật từ Entity
                        e.getCategory() != null ? e.getCategory().getName() : "Chung",
                        e.getRound().getId(),
                        e.getTotalScore().doubleValue(),
                        e.getRankPosition(),
                        e.getIsPromoted()
                ))
                .collect(Collectors.toList());
    }

    // Hãy import thêm: import org.springframework.jdbc.core.JdbcTemplate;
    // Và khai báo tiêm: private final JdbcTemplate jdbcTemplate; (nếu file chưa có)

    @Override
    @Transactional
    public void disqualifyTeam(Long teamId, String reason, Long userId) {
        log.info("Coordinator {} đang đình chỉ Team {} với lý do: {}", userId, teamId, reason);

        // 1. Cập nhật trạng thái đội thành DISQUALIFIED
        String updateSql = "UPDATE teams SET status = 'DISQUALIFIED' WHERE id = ?";
        jdbcTemplate.update(updateSql, teamId);

        // 2. (Tùy chọn) Ghi vào bảng Audit Log để đáp ứng FR-RNK-05
        // String auditSql = "INSERT INTO audit_logs (action, target_id, user_id, reason, created_at) VALUES (?, ?, ?, ?, NOW())";
        // jdbcTemplate.update(auditSql, "DISQUALIFY_TEAM", teamId, userId, reason);

        // 3. Xóa các bản ghi xếp hạng hiện tại của đội này (nếu có)
        // rankingRepository.deleteByTeamId(teamId);
    }
}