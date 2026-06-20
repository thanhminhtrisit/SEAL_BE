package com.seal.seal_backend.ranking.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingDataProvider {

    private final JdbcTemplate jdbcTemplate;

    // 1. CẬP NHẬT RECORD: Thêm categoryId và categoryName
    public record TeamView(Long id, String name, String status, Long categoryId, String categoryName) {}
    public record CriterionView(Long id, Double weight) {}
    public record ScoreView(Long teamId, Long criterionId, Double scoreValue) {}

    // 2. CẬP NHẬT HÀM: Thêm LEFT JOIN bảng categories
    public List<TeamView> getTeamsInRound(Long roundId) {
        String sql = "SELECT t.id, t.name, t.status, t.category_id, c.name AS category_name " +
                "FROM teams t " +
                "JOIN submissions s ON s.team_id = t.id " +
                "LEFT JOIN categories c ON t.category_id = c.id " + // LEFT JOIN để đề phòng team chưa có category
                "WHERE s.round_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeamView(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("status"),

                // Dùng rs.getObject thay vì rs.getLong để nếu trong DB là NULL thì Java sẽ nhận được giá trị null (không bị ép thành số 0)
                rs.getObject("category_id", Long.class),

                rs.getString("category_name")
        ), roundId);
    }

    // Lấy tiêu chí và trọng số của round này (Giữ nguyên)
    public List<CriterionView> getCriteriaForRound(Long roundId) {
        String sql = "SELECT sc.id, sc.weight FROM scoring_criteria sc " +
                "JOIN criteria_sets cs ON cs.id = sc.criteria_set_id " +
                "WHERE cs.round_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CriterionView(
                rs.getLong("id"), rs.getDouble("weight")
        ), roundId);
    }

    // Lấy toàn bộ điểm số của các giám khảo (Giữ nguyên)
    public List<ScoreView> getScoresForRound(Long roundId) {
        String sql = "SELECT s.team_id, sc.criterion_id, sc.score_value " +
                "FROM scores sc " +
                "JOIN evaluations e ON e.id = sc.evaluation_id " +
                "JOIN submission_versions sv ON sv.id = e.submission_version_id " +
                "JOIN submissions s ON s.id = sv.submission_id " +
                "WHERE e.round_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ScoreView(
                rs.getLong("team_id"), rs.getLong("criterion_id"), rs.getDouble("score_value")
        ), roundId);
    }
}