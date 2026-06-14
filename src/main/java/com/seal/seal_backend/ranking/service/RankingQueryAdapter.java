package com.seal.seal_backend.ranking.service;

import com.seal.seal_backend.shared.contract.RankingQueryPort;
import com.seal.seal_backend.shared.contract.dto.RankingView;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

/** OWNER: M3. Stub of RankingQueryPort. */
@Service
public class RankingQueryAdapter implements RankingQueryPort {
    @Override public List<RankingView> rankingForRound(Long roundId, Long categoryId) { return Collections.emptyList(); }
    @Override public boolean isTeamPromoted(Long teamId, Long roundId) { return false; }
}
