package com.seal.seal_backend.award.service.impl;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import com.seal.seal_backend.award.service.AwardService;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.AwardType;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.repository.AwardRepository;
import com.seal.seal_backend.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
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

        // Chặn một đội nhận nhiều hơn một giải bất kỳ trong cùng một sự kiện
        // 1. NGĂN CHẶN NHẬN TRÙNG CHÍNH XÁC 1 LOẠI GIẢI
        String checkExactAwardSql = "SELECT COUNT(*) FROM awards WHERE event_id = ? AND team_id = ? AND award_type = ?";
        Integer exactCount = jdbcTemplate.queryForObject(checkExactAwardSql, Integer.class, request.eventId(), request.teamId(), request.awardType().name());
        if (exactCount != null && exactCount > 0) {
            throw new RuntimeException("Lỗi: Đội '" + team.getName() + "' đã nhận giải '" + request.awardType() + "' rồi!");
        }

        // 2. NGĂN CHẶN NHẬN NHIỀU HƠN 1 GIẢI CHÍNH (Nhất/Nhì/Ba)
        boolean isRequestingMainAward = isMainAward(request.awardType());
        if (isRequestingMainAward) {
            String checkMainAwardSql = "SELECT COUNT(*) FROM awards WHERE event_id = ? AND team_id = ? " +
                    "AND award_type IN ('FIRST_PLACE', 'SECOND_PLACE', 'THIRD_PLACE')";
            Integer mainCount = jdbcTemplate.queryForObject(checkMainAwardSql, Integer.class, request.eventId(), request.teamId());
            if (mainCount != null && mainCount > 0) {
                throw new RuntimeException("Lỗi: Đội '" + team.getName() + "' đã có một Giải Chính (Nhất/Nhì/Ba) rồi, không thể nhận thêm Giải Chính khác!");
            }
        }

        // Chặn cùng một loại giải bị trao cho nhiều team trong cùng event
        String checkCategoryAwardSql = "SELECT COUNT(*) FROM awards a " +
                "JOIN teams t ON a.team_id = t.id " +
                "WHERE a.event_id = ? AND t.category_id = ? AND a.award_type = ?";

        Integer awardCount = jdbcTemplate.queryForObject(
                checkCategoryAwardSql,
                Integer.class,
                request.eventId(),
                request.categoryId(),
                request.awardType().name()
        );

        if (awardCount != null && awardCount > 0) {
            throw new RuntimeException("Lỗi: Giải '" + request.awardType() + "' đã được trao cho Hạng mục này rồi!");
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

        String eventName = jdbcTemplate.queryForObject(
                "SELECT name FROM events WHERE id = ?", String.class, request.eventId());
        String categoryName = jdbcTemplate.queryForObject(
                "SELECT name FROM categories WHERE id = ?", String.class, request.categoryId());

        return new AwardResponse(
                savedAward.getId(),
                request.eventId(),
                eventName,
                request.teamId(),
                team.getName(),
                categoryName,
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
                        a.getEvent().getName(),                     // Lấy tên sự kiện
                        a.getTeam().getId(),
                        a.getTeam().getName(),
                        a.getTeam().getCategory().getName(),        // Lấy tên hạng mục
                        a.getAwardType(),
                        a.getDescription(),
                        a.getAwardedBy().getId(),
                        a.getAwardedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getAwardTypes() {
        return List.of(
                buildAwardTypeOption(AwardType.FIRST_PLACE, "Giải Nhất", true),
                buildAwardTypeOption(AwardType.SECOND_PLACE, "Giải Nhì", true),
                buildAwardTypeOption(AwardType.THIRD_PLACE, "Giải Ba", true),
                buildAwardTypeOption(AwardType.BEST_TECHNICAL, "Giải Kỹ Thuật", false),
                buildAwardTypeOption(AwardType.BEST_PRESENTATION, "Giải Thuyết Trình", false),
                buildAwardTypeOption(AwardType.SPECIAL, "Giải Khuyến Khích", false)
        );
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
        log.info("Coordinator (ID:{}) đang CÔNG BỐ KẾT QUẢ và chuyển trạng thái sự kiện ID: {} thành COMPLETED", userId, eventId);

        // Chỉ cập nhật duy nhất trường status
        String sql = "UPDATE events SET status = ? WHERE id = ?";

        jdbcTemplate.update(sql, EventStatus.COMPLETED.name(), eventId);
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

    private boolean isMainAward(AwardType awardType) {
        return awardType == AwardType.FIRST_PLACE
                || awardType == AwardType.SECOND_PLACE
                || awardType == AwardType.THIRD_PLACE;
    }

    private Map<String, Object> buildAwardTypeOption(AwardType awardType, String label, boolean isMainAward) {
        Map<String, Object> option = new HashMap<>();
        option.put("code", awardType.name());
        option.put("label", label);
        option.put("isMainAward", isMainAward);
        return option;
    }


    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getSuggestedAwards(Long eventId, Long categoryId) {
        // 1. Lấy round cuối cùng (giống logic getEligibleTeamsForAward của bạn)
        String roundSql = "SELECT id FROM rounds WHERE event_id = ? ORDER BY is_final_round DESC, order_number DESC LIMIT 1";
        List<Map<String, Object>> roundRows = jdbcTemplate.queryForList(roundSql, eventId);
        if (roundRows.isEmpty()) return List.of();
        Long roundId = ((Number) roundRows.get(0).get("id")).longValue();

        // 2. Truy vấn Top 3 đội (Rank 1, 2, 3)
        String sql = "SELECT t.id AS teamId, t.name AS teamName, rk.rank_position AS rankPosition, " +
                "CASE " +
                "  WHEN rk.rank_position = 1 THEN 'FIRST_PLACE' " +
                "  WHEN rk.rank_position = 2 THEN 'SECOND_PLACE' " +
                "  WHEN rk.rank_position = 3 THEN 'THIRD_PLACE' " +
                "  ELSE NULL END AS suggestedAwardType " +
                "FROM rankings rk " +
                "JOIN teams t ON rk.team_id = t.id " +
                "WHERE rk.round_id = ? AND t.category_id = ? AND rk.rank_position <= 3 " +
                "ORDER BY rk.rank_position ASC";

        return jdbcTemplate.queryForList(sql, roundId, categoryId);
    }
}