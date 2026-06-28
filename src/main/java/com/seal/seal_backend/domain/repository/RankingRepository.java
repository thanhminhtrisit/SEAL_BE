package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // Tìm qua thuộc tính 'round.id' thay vì 'roundId'
    @Query("SELECT r FROM Ranking r WHERE r.round.id = :roundId ORDER BY r.rankPosition ASC")
    List<Ranking> findByRoundIdOrderByRankPositionAsc(@Param("roundId") Long roundId);

    // Xóa thông qua object round
    @Modifying
    @Query("DELETE FROM Ranking r WHERE r.round.id = :roundId")
    void deleteByRoundId(@Param("roundId") Long roundId);

    boolean existsByRoundIdAndTeamIdAndIsPromotedTrue(Long roundId, Long teamId);

    boolean existsByRoundIdAndTeamId(Long roundId, Long teamId);
}
