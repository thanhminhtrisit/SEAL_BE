package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

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
}
