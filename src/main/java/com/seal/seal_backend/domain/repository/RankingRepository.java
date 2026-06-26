package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    boolean existsByRoundIdAndTeamIdAndIsPromotedTrue(Long roundId, Long teamId);

    boolean existsByRoundIdAndTeamId(Long roundId, Long teamId);
}
