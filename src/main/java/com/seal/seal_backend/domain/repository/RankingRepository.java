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

    @Query("SELECT r FROM Ranking r WHERE r.round.id = :roundId ORDER BY r.rankPosition ASC")
    List<Ranking> findByRoundIdOrderByRankPositionAsc(@Param("roundId") Long roundId);

    @Modifying
    @Query("DELETE FROM Ranking r WHERE r.round.id = :roundId")
    void deleteByRoundId(@Param("roundId") Long roundId);

    boolean existsByRoundIdAndTeamIdAndIsPromotedTrue(Long roundId, Long teamId);

    boolean existsByRoundIdAndTeamId(Long roundId, Long teamId);

    @Modifying
    @Query("UPDATE Ranking r SET r.isPromoted = true WHERE r.round.id = :roundId AND r.team.id IN :teamIds")
    void markTeamsAsPromoted(Long roundId, List<Long> teamIds);
}
