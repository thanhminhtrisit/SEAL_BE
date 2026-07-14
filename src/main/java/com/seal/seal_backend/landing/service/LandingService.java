package com.seal.seal_backend.landing.service;

import com.seal.seal_backend.landing.dto.LandingResponse;
import com.seal.seal_backend.landing.dto.LandingResponse.EventCard;
import com.seal.seal_backend.landing.dto.LandingResponse.Stats;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Read-only aggregation for the PUBLIC landing page. No authentication required, so it exposes
 * only non-sensitive summary data about events that are OPEN for registration.
 */
@Service
@RequiredArgsConstructor
public class LandingService {

    private final JdbcTemplate jdbc;

    // A team "counts" toward an event once it has registered and while it stays in the running.
    private static final String ACTIVE_TEAM_STATUSES = "('REGISTERED','APPROVED','ACTIVE')";

    @Transactional(readOnly = true)
    public LandingResponse getLanding() {
        long openEvents = count("SELECT COUNT(*) FROM events WHERE status = 'OPEN'");
        long registeredTeams = count(
                "SELECT COUNT(*) FROM teams t JOIN events e ON t.event_id = e.id " +
                "WHERE e.status = 'OPEN' AND t.status IN " + ACTIVE_TEAM_STATUSES);
        long categories = count(
                "SELECT COUNT(*) FROM categories c JOIN events e ON c.event_id = e.id " +
                "WHERE e.status = 'OPEN'");
        long judges = count(
                "SELECT COUNT(*) FROM users u JOIN roles r ON u.primary_role_id = r.id " +
                "WHERE r.code = 'JUDGE' AND u.status = 'ACTIVE'");

        List<EventCard> events = jdbc.query(
                "SELECT e.id, e.name, e.event_type, e.registration_start, e.registration_end, " +
                "       e.max_teams, d.name AS discipline_name, " +
                "       (SELECT COUNT(*) FROM teams t WHERE t.event_id = e.id " +
                "        AND t.status IN " + ACTIVE_TEAM_STATUSES + ") AS registered_teams " +
                "FROM events e JOIN disciplines d ON e.discipline_id = d.id " +
                "WHERE e.status = 'OPEN' " +
                "ORDER BY e.registration_end ASC",
                (rs, i) -> {
                    Long id = rs.getLong("id");
                    List<String> cats = jdbc.queryForList(
                            "SELECT name FROM categories WHERE event_id = ? ORDER BY name", String.class, id);
                    Timestamp start = rs.getTimestamp("registration_start");
                    Timestamp end = rs.getTimestamp("registration_end");
                    Object maxTeamsObj = rs.getObject("max_teams");
                    Integer maxTeams = maxTeamsObj == null ? null : ((Number) maxTeamsObj).intValue();
                    return new EventCard(
                            id,
                            rs.getString("name"),
                            rs.getString("discipline_name"),
                            rs.getString("event_type"),
                            start == null ? null : start.toLocalDateTime(),
                            end == null ? null : end.toLocalDateTime(),
                            maxTeams,
                            rs.getLong("registered_teams"),
                            cats);
                });

        return new LandingResponse(new Stats(openEvents, registeredTeams, categories, judges), events);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0L : n;
    }
}
