package com.seal.seal_backend.award.service.impl;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.repository.AwardRepository;
import com.seal.seal_backend.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {

    private final AwardRepository awardRepository;
    private final TeamRepository teamRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public AwardResponse createAward(AwardCreateRequest request, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Không thể xác thực danh tính Người điều phối (User ID is null). Vui lòng đăng nhập lại!");
        }


        //Kiểm tra đội thi có tồn tại không
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội thi với ID: " + request.teamId()));

        //Chặn trao giải cho đội đã bị đình chỉ
        if ("DISQUALIFIED".equals(team.getStatus())) {
            throw new RuntimeException("Lỗi: Đội thi '" + team.getName() + "' đã bị đình chỉ và không đủ điều kiện nhận giải!");
        }

        //Chống Spam: Kiểm tra đội thi đã nhận giải thưởng này trong sự kiện này chưa
        boolean isAlreadyAwarded = awardRepository.existsByEventIdAndTeamIdAndAwardType(
                request.eventId(),
                request.teamId(),
                request.awardType()
        );

        if (isAlreadyAwarded) {
            throw new RuntimeException("Lỗi: Đội '" + team.getName() + "' đã được trao giải '" + request.awardType() + "' rồi!");
        }

        log.info("Coordinator (ID:{}) đang gán giải {} cho Team ID: {}", userId, request.awardType(), request.teamId());

        Event eventRef = new Event(); eventRef.setId(request.eventId());
        User userRef = new User(); userRef.setId(userId);

        Ranking rankingRef = null;
        if (request.rankingId() != null) {
            rankingRef = new Ranking();
            rankingRef.setId(request.rankingId());
        }

        Award award = Award.builder()
                .event(eventRef)
                .team(team)
                .ranking(rankingRef)
                .awardType(request.awardType())
                .description(request.description())
                .awardedBy(userRef)
                .build();

        Award savedAward = awardRepository.save(award);

        return new AwardResponse(
                savedAward.getId(),
                request.eventId(),
                request.teamId(),
                team.getName(),
                savedAward.getAwardType(),
                savedAward.getDescription(),
                userId,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AwardResponse> getAwardsByEvent(Long eventId) {
        log.info("Lấy danh sách giải thưởng của Event ID: {}", eventId);

        return awardRepository.findByEventIdOrderByAwardedAtDesc(eventId)
                .stream()
                .map(a -> new AwardResponse(
                        a.getId(),
                        a.getEvent().getId(),
                        a.getTeam().getId(),
                        a.getTeam().getName(),
                        a.getAwardType(),
                        a.getDescription(),
                        a.getAwardedBy().getId(),
                        a.getAwardedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getEligibleTeamsForAward(Long eventId, Long categoryId) {
        String roundSql = "SELECT id FROM rounds " +
                "WHERE event_id = ? " +
                "ORDER BY is_final_round DESC, order_number DESC, id DESC " +
                "LIMIT 1";

        List<Map<String, Object>> roundRows = jdbcTemplate.queryForList(roundSql, eventId);
        if (roundRows.isEmpty()) {
            return List.of();
        }

        Long eligibleRoundId = ((Number) roundRows.get(0).get("id")).longValue();

        String sql = "SELECT t.id AS teamId, t.name AS teamName, " +
                "rk.rank_position AS rankPosition, rk.total_score AS totalScore " +
                "FROM rankings rk " +
                "JOIN teams t ON rk.team_id = t.id " +
                "WHERE rk.round_id = ? " +
                "AND t.category_id = ? " +
                "ORDER BY rk.rank_position ASC";

        List<Map<String, Object>> rawResult = jdbcTemplate.queryForList(sql, eligibleRoundId, categoryId);

        return rawResult.stream().map(row -> {
            Map<String, Object> formattedRow = new HashMap<>();
            formattedRow.put("teamId", row.getOrDefault("teamId", row.get("TEAMID")));
            formattedRow.put("teamName", row.getOrDefault("teamName", row.get("TEAMNAME")));
            formattedRow.put("rankPosition", row.getOrDefault("rankPosition", row.get("RANKPOSITION")));
            formattedRow.put("totalScore", row.getOrDefault("totalScore", row.get("TOTALSCORE")));
            return formattedRow;
        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void publishEventResults(Long eventId, Long userId) {
        log.info("Coordinator (ID:{}) đang CÔNG BỐ KẾT QUẢ sự kiện ID: {}", userId, eventId);
        String sql = "UPDATE events SET is_results_published = true WHERE id = ?";
        jdbcTemplate.update(sql, eventId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getCategoriesByEvent(Long eventId) {
        String sql = "SELECT id, name FROM categories WHERE event_id = ?";
        List<Map<String, Object>> rawResult = jdbcTemplate.queryForList(sql, eventId);

        return rawResult.stream().map(row -> {
            Map<String, Object> formattedRow = new HashMap<>();
            formattedRow.put("id", row.getOrDefault("id", row.get("ID")));
            formattedRow.put("name", row.getOrDefault("name", row.get("NAME")));
            return formattedRow;
        }).collect(Collectors.toList());
    }
}