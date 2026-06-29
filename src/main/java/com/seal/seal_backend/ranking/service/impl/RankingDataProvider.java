package com.seal.seal_backend.ranking.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RankingDataProvider {

    private final JdbcTemplate jdbcTemplate;

    // 1. CẬP NHẬT RECORD: Thêm submissionTime
    public record TeamView(Long id, String name, String status, Long categoryId, String categoryName, LocalDateTime submissionTime) {}
    public record CriterionView(Long id, Double weight) {}
    public record ScoreView(Long teamId, Long criterionId, Double scoreValue) {}

    public List<TeamView> getTeamsInRound(Long roundId) {
        // Lấy thêm s.created_at AS submission_time
        String sql = "SELECT t.id, t.name, t.status, t.category_id, c.name AS category_name, s.created_at AS submission_time " +
                "FROM teams t " +
                "JOIN submissions s ON s.team_id = t.id " +
                "LEFT JOIN categories c ON t.category_id = c.id " +
                "WHERE s.round_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeamView(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("status"),
                rs.getObject("category_id", Long.class),
                rs.getString("category_name"),
                rs.getObject("submission_time", LocalDateTime.class) // Lấy thời gian nộp bài
        ), roundId);
    }

    // Các hàm getCriteriaForRound và getScoresForRound giữ nguyên như cũ của bạn
    public List<CriterionView> getCriteriaForRound(Long roundId) {
        String sql = "SELECT sc.id, sc.weight FROM scoring_criteria sc JOIN criteria_sets cs ON cs.id = sc.criteria_set_id WHERE cs.round_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CriterionView(rs.getLong("id"), rs.getDouble("weight")), roundId);
    }

    public List<ScoreView> getScoresForRound(Long roundId) {
        String sql = "SELECT s.team_id, sc.criterion_id, sc.score_value FROM scores sc JOIN evaluations e ON e.id = sc.evaluation_id JOIN submission_versions sv ON sv.id = e.submission_version_id JOIN submissions s ON s.id = sv.submission_id WHERE e.round_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ScoreView(rs.getLong("team_id"), rs.getLong("criterion_id"), rs.getDouble("score_value")), roundId);
    }
}