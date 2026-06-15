package com.seal.seal_backend.ranking.service;

import com.seal.seal_backend.ranking.dto.response.RankingResponse;

import java.util.List;

public interface RankingService {
    /**
     * Kích hoạt tính toán điểm và xếp hạng (Áp dụng BR-RNK-01, 02, 04, 05)
     */
    List<RankingResponse> computeRankingForRound(Long roundId, Long userId);

    /**
     * Lấy danh sách kết quả xếp hạng
     */
    List<RankingResponse> getRankingsByRound(Long roundId);
}
