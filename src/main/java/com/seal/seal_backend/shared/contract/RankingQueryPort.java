package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.RankingView;
import java.util.List;

/** IMPLEMENTED BY: ranking module (M3). CONSUMED BY: award (M3), submission (next-round gate). */
public interface RankingQueryPort {
    List<RankingView> rankingForRound(Long roundId, Long categoryId);
    /** BR-SUB-05: only promoted teams may submit in the next round. */
    boolean isTeamPromoted(Long teamId, Long roundId);
}
