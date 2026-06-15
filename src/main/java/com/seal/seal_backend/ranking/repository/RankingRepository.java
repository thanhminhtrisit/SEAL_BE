package com.seal.seal_backend.ranking.repository;

import com.seal.seal_backend.domain.entity.Ranking;

import java.util.List;

public interface RankingRepository {
    // Lấy danh sách xếp hạng của một vòng thi, sắp xếp theo thứ hạng (1, 2, 3...)
    List<Ranking> findByRoundIdOrderByRankPositionAsc(Long roundId);

    // Xóa xếp hạng cũ của một vòng thi (khi coordinator bấm tính toán lại)
    void deleteByRoundId(Long roundId);
}
