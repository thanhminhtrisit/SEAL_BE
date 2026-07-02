package com.seal.seal_backend.ranking.service.impl;

import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.repository.RankingRepository;
import com.seal.seal_backend.domain.repository.RoundRepository;
import com.seal.seal_backend.domain.repository.SubmissionRepository;
import com.seal.seal_backend.ranking.dto.response.CategoryResponse;
import com.seal.seal_backend.ranking.dto.response.RankingResponse;
import com.seal.seal_backend.ranking.dto.response.DisqualifiedTeamResponse;
import com.seal.seal_backend.ranking.dto.response.ScoreBreakdownResponse;
import com.seal.seal_backend.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final RankingRepository rankingRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final RankingDataProvider dataProvider;
    private final JdbcTemplate jdbcTemplate;

    private static class TeamScoreData {
        RankingDataProvider.TeamView team;
        double totalScore;
        Map<Long, Double> scoresByCriterion;

        public TeamScoreData(RankingDataProvider.TeamView team, double totalScore, Map<Long, Double> scoresByCriterion) {
            this.team = team;
            this.totalScore = totalScore;
            this.scoresByCriterion = scoresByCriterion;
        }
    }

    @Override
    @Transactional
    public List<RankingResponse> computeRankingForRound(Long roundId, Long categoryId, Long userId) {
        log.info("Bắt đầu điều phối tiến trình tính toán xếp hạng cho round: {}, category: {}", roundId, categoryId);

        Round currentRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi với ID: " + roundId));

        List<RankingDataProvider.CriterionView> criteria = dataProvider.getCriteriaForRound(roundId);
        List<RankingDataProvider.ScoreView> allScores = dataProvider.getScoresForRound(roundId);
        validateScoringData(criteria, allScores);

        List<RankingDataProvider.TeamView> allTeams = dataProvider.getTeamsInRound(roundId);

        List<RankingDataProvider.TeamView> validTeams = allTeams.stream()
                .filter(team -> !"DISQUALIFIED".equals(team.status()))
                .filter(team -> {
                    if (categoryId == null || categoryId == 0) return true; // Lấy tất cả
                    Long teamCatId = team.categoryId() != null ? team.categoryId() : 0L;
                    return teamCatId.equals(categoryId); // Chỉ lấy team đúng category
                })
                .toList();

        if (validTeams.isEmpty()) {
            log.warn("Không có đội thi nào hợp lệ để xếp hạng cho Category ID: {}", categoryId);
            return Collections.emptyList();
        }

        List<TeamScoreData> rankedTeamsData = calculateAndSortRankings(validTeams, criteria, allScores);

        if (categoryId == null || categoryId == 0) {
            log.info("Xóa toàn bộ bảng xếp hạng cũ của vòng thi: {}", roundId);
            rankingRepository.deleteByRoundId(roundId);
        } else {
            log.info("Chỉ xóa bảng xếp hạng cũ của category: {} trong vòng thi: {}", categoryId, roundId);
            jdbcTemplate.update("DELETE FROM rankings WHERE round_id = ? AND category_id = ?", roundId, categoryId);
        }

        List<Ranking> savedEntities = saveRankingsToDatabase(rankedTeamsData, currentRound, userId);

        return mapToRankingResponse(savedEntities, validTeams);
    }

    private void validateScoringData(List<RankingDataProvider.CriterionView> criteria, List<RankingDataProvider.ScoreView> allScores) {
        if (criteria.isEmpty()) {
            log.warn("CẢNH BÁO: Vòng thi chưa được cấu hình tiêu chí (Criteria). Các đội sẽ nhận 0 điểm và xếp hạng theo Tie-breaker mặc định.");
        }

        if (allScores.isEmpty()) {
            log.warn("CẢNH BÁO: Chưa có bất kỳ dữ liệu chấm điểm (Evaluations/Scores) nào. Bảng xếp hạng sẽ được khởi tạo với điểm 0.");
        }
    }

    private List<TeamScoreData> calculateAndSortRankings(
            List<RankingDataProvider.TeamView> validTeams,
            List<RankingDataProvider.CriterionView> criteria,
            List<RankingDataProvider.ScoreView> allScores) {

        List<RankingDataProvider.CriterionView> sortedCriteriaByWeight = criteria.stream()
                .sorted((c1, c2) -> Double.compare(c2.weight(), c1.weight()))
                .toList();

        Map<Long, List<RankingDataProvider.ScoreView>> scoresGroupedByTeam = allScores.stream()
                .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::teamId));

        // Gom nhóm các đội theo Category ID (Nếu null mặc định gom vào nhóm 0L - Chung)
        Map<Long, List<RankingDataProvider.TeamView>> teamsByCategory = validTeams.stream()
                .collect(Collectors.groupingBy(t -> t.categoryId() != null ? t.categoryId() : 0L));

        List<TeamScoreData> globalRankedList = new ArrayList<>();

        // Xử lý tính toán độc lập và áp dụng Tie-breaker riêng biệt bên trong mỗi Hạng mục (Category)
        for (Map.Entry<Long, List<RankingDataProvider.TeamView>> entry : teamsByCategory.entrySet()) {
            List<RankingDataProvider.TeamView> teamsInGroup = entry.getValue();
            List<TeamScoreData> categoryScores = new ArrayList<>();

            for (RankingDataProvider.TeamView team : teamsInGroup) {
                List<RankingDataProvider.ScoreView> teamScores = scoresGroupedByTeam.getOrDefault(team.id(), List.of());

                Map<Long, Double> avgScoreByCriterion = teamScores.stream()
                        .collect(Collectors.groupingBy(RankingDataProvider.ScoreView::criterionId,
                                Collectors.averagingDouble(RankingDataProvider.ScoreView::scoreValue)));

                double finalScore = 0.0;
                for (RankingDataProvider.CriterionView criterion : criteria) {
                    double avgScore = avgScoreByCriterion.getOrDefault(criterion.id(), 0.0);
                    finalScore += avgScore * (criterion.weight() / 100.0);
                }

                categoryScores.add(new TeamScoreData(team, finalScore, avgScoreByCriterion));
            }

            categoryScores.sort((d1, d2) -> {
                int cmp = Double.compare(d2.totalScore, d1.totalScore);
                if (cmp != 0) return cmp;

                for (RankingDataProvider.CriterionView crit : sortedCriteriaByWeight) {
                    double s1 = d1.scoresByCriterion.getOrDefault(crit.id(), 0.0);
                    double s2 = d2.scoresByCriterion.getOrDefault(crit.id(), 0.0);
                    cmp = Double.compare(s2, s1);
                    if (cmp != 0) return cmp;
                }

                if (d1.team.submissionTime() != null && d2.team.submissionTime() != null) {
                    cmp = d1.team.submissionTime().compareTo(d2.team.submissionTime());
                    if (cmp != 0) return cmp;
                } else if (d1.team.submissionTime() != null) {
                    return -1;
                } else if (d2.team.submissionTime() != null) {
                    return 1;
                }

                return d1.team.id().compareTo(d2.team.id());
            });

            globalRankedList.addAll(categoryScores);
        }

        return globalRankedList;
    }

    private List<Ranking> saveRankingsToDatabase(List<TeamScoreData> rankedTeamsData, Round currentRound, Long userId) {
        List<Ranking> entitiesToSave = new ArrayList<>();

        Map<Long, Integer> categoryRankCounter = new HashMap<>();

        for (TeamScoreData data : rankedTeamsData) {
            Long catId = data.team.categoryId() != null ? data.team.categoryId() : 0L;
            int currentRank = categoryRankCounter.getOrDefault(catId, 1);

            // MỤC TIÊU 3: XÁC NHẬN QUY TẮC PROMOTION_TOP_N THEO ROUND TRẠNG THÁI FINAL
            boolean isPromoted = false;
            if (Boolean.FALSE.equals(currentRound.getIsFinalRound())) {
                Integer topN = currentRound.getPromotionTopN();
                if (topN != null && currentRank <= topN) {
                    isPromoted = true;
                }
            }

            Event eventRef = new Event(); eventRef.setId(currentRound.getEvent().getId());
            Round roundRef = new Round(); roundRef.setId(currentRound.getId());
            Team teamRef = new Team(); teamRef.setId(data.team.id());

            User userRef = null;
            if (userId != null) {
                userRef = new User(); userRef.setId(userId);
            }

            Category categoryRef = null;
            if (!catId.equals(0L)) {
                categoryRef = new Category(); categoryRef.setId(catId);
            }

            Ranking entity = new Ranking();
            entity.setEvent(eventRef);
            entity.setRound(roundRef);
            entity.setCategory(categoryRef);
            entity.setTeam(teamRef);
            entity.setTotalScore(BigDecimal.valueOf(data.totalScore));
            entity.setRankPosition(currentRank);
            entity.setIsPromoted(isPromoted);
            entity.setComputedBy(userRef);
            entity.setSnapshotNote("Ranked sequentially inside category group");

            entitiesToSave.add(entity);

            // Cập nhật lại số hạng tiếp theo cho Category này
            categoryRankCounter.put(catId, currentRank + 1);
        }

        return rankingRepository.saveAll(entitiesToSave);
    }

    private List<RankingResponse> mapToRankingResponse(List<Ranking> savedEntities, List<RankingDataProvider.TeamView> validTeams) {
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
        log.info("Coordinator {} đang tiến hành đình chỉ Team {} với lý do: {}", userId, teamId, reason);

        String updateTeamSql = "UPDATE teams SET status = 'DISQUALIFIED', " +
                "disqualified_reason = ?, " +
                "disqualified_by = ?, " +
                "disqualified_at = NOW() WHERE id = ?";
        jdbcTemplate.update(updateTeamSql, reason, userId, teamId);

        // Thực hiện cơ chế Soft Delete đóng băng bài nộp rỗng của các vòng kế tiếp phục vụ đối chứng tra cứu lịch sử
        String cancelSubmissionsSql =
                "UPDATE submissions s " +
                        "LEFT JOIN evaluations e ON s.id = e.submission_id " +
                        "SET s.status = 'DISQUALIFIED' " +
                        "WHERE s.team_id = ? AND e.id IS NULL";

        int updatedCount = jdbcTemplate.update(cancelSubmissionsSql, teamId);
        log.info("Marked {} unscored submissions as disqualified.", updatedCount);
    }


    @Override
    @Transactional(readOnly = true)
    public List<DisqualifiedTeamResponse> getDisqualifiedTeams(Long eventId) {
        log.info("Truy xuất danh sách DTO các đội bị đình chỉ cho Event ID: {}", eventId);
        String sql = "SELECT id, name, disqualified_reason AS disqualifiedReason FROM teams WHERE event_id = ? AND status = 'DISQUALIFIED'";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(DisqualifiedTeamResponse.class), eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreBreakdownResponse> getScoreBreakdown(Long teamId, Long roundId) {
        log.info("Truy xuất chi tiết bảng điểm DTO (Score Breakdown) cho Team ID: {} tại Round ID: {}", teamId, roundId);
        String sql = "SELECT u.name AS judgeName, sc.name AS criterionName, sc.weight AS criterionWeight, " +
                "s.score_value AS scoreValue, s.comment AS judgeComment " +
                "FROM scores s " +
                "JOIN evaluations e ON s.evaluation_id = e.id " +
                "JOIN submissions sub ON e.submission_id = sub.id " +
                "JOIN scoring_criteria sc ON s.criterion_id = sc.id " +
                "JOIN users u ON e.judge_id = u.id " +
                "WHERE sub.team_id = ? AND e.round_id = ? " +
                "ORDER BY u.name ASC, sc.weight DESC";

        return jdbcTemplate.query(sql, new DataClassRowMapper<>(ScoreBreakdownResponse.class), teamId, roundId);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByEvent(Long eventId) {
        String sql = "SELECT DISTINCT c.id, c.name FROM categories c " +
                "JOIN teams t ON c.id = t.category_id " +
                "WHERE t.event_id = ?";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(CategoryResponse.class), eventId);
    }

    @Override
    @Transactional
    public void promoteTeamsToNextRound(Long currentRoundId, List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return;
        }

        Round current = roundRepository.findById(currentRoundId)
                .orElseThrow(() -> new RuntimeException("Round không tồn tại"));

        List<Round> nextRounds = roundRepository.findNextRounds(current.getEvent().getId(), current.getOrderNumber());
        if (nextRounds.isEmpty()) {
            throw new RuntimeException("Đây đã là vòng thi cuối cùng, không thể thăng hạng!");
        }
        Round nextRound = nextRounds.get(0);

        rankingRepository.markTeamsAsPromoted(currentRoundId, teamIds);

        List<Long> existingTeamIds = submissionRepository.findExistingSubmissionTeamIds(nextRound.getId(), teamIds);

        List<Long> teamsToCreate = teamIds.stream()
                .filter(id -> !existingTeamIds.contains(id))
                .collect(Collectors.toList());

        for (Long teamId : teamsToCreate) {
            submissionRepository.createPlaceholderSubmission(teamId, nextRound.getId());
        }

        log.info("Đã thăng hạng thành công {} đội sang vòng {}", teamIds.size(), nextRound.getName());
    }
}
