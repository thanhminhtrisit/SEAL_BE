package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Round;
import com.seal.seal_backend.domain.enums.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByEventIdOrderByOrderNumber(Long eventId);
    boolean existsByEventIdAndOrderNumber(Long eventId, Integer orderNumber);
    @Query("SELECT r FROM Round r WHERE r.event.id = :eventId AND r.orderNumber > :currentOrderNumber ORDER BY r.orderNumber ASC")
    List<Round> findNextRounds(@Param("eventId") Long eventId, @Param("currentOrderNumber") Integer currentOrderNumber);

    // Hàm helper để lấy vòng kế tiếp duy nhất
    default Round findNextRound(Long eventId, Integer currentOrderNumber) {
        List<Round> nextRounds = findNextRounds(eventId, currentOrderNumber);
        return nextRounds.isEmpty() ? null : nextRounds.get(0);
    }

    List<Round> findByEventIdOrderByOrderNumberAsc(Long eventId);

    Optional<Round> findByEventIdAndOrderNumber(Long eventId, Integer orderNumber);

    List<Round> findByStatusAndScoringDeadlineBefore(RoundStatus status, LocalDateTime now);

    List<Round> findByStatusAndScoringDeadlineBeforeOrderByOrderNumberAsc(RoundStatus status, LocalDateTime now);

    /**
     * Make room for a new round at {@code pos}: shift every round at/after {@code pos} up by 1.
     * Immediate bulk DML — {@code flushAutomatically} flushes pending changes first so it runs
     * before the new row's INSERT, and {@code clearAutomatically} drops now-stale managed entities.
     * ORDER BY order_number DESC updates the highest slot first, so the per-row unique check on
     * (event_id, order_number) never sees a transient duplicate (matters on MySQL/InnoDB).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE rounds SET order_number = order_number + 1 "
            + "WHERE event_id = :eventId AND order_number >= :pos ORDER BY order_number DESC",
            nativeQuery = true)
    void shiftOrdersUp(@Param("eventId") Long eventId, @Param("pos") int pos);

    /**
     * Close the gap left by a deleted round at {@code pos}: shift every round after {@code pos}
     * down by 1. ORDER BY order_number ASC fills the freed slot first, avoiding transient
     * duplicate-key collisions during the multi-row UPDATE.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE rounds SET order_number = order_number - 1 "
            + "WHERE event_id = :eventId AND order_number > :pos ORDER BY order_number ASC",
            nativeQuery = true)
    void shiftOrdersDown(@Param("eventId") Long eventId, @Param("pos") int pos);
}
