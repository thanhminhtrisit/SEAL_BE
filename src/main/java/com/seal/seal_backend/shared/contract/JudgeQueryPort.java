package com.seal.seal_backend.shared.contract;

import java.util.List;

/** IMPLEMENTED BY: event module (M1). CONSUMED BY: scoring (M2), ranking (M3). */
public interface JudgeQueryPort {
    boolean isJudgeAssignedToRound(Long judgeId, Long roundId);
    List<Long> judgeIdsForRound(Long roundId);
}
