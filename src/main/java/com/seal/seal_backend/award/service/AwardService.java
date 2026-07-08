package com.seal.seal_backend.award.service;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface AwardService {

    // Gán một giải thưởng cho một Team
    AwardResponse createAward(AwardCreateRequest request, Long userId);

    // Lấy danh sách giải thưởng của một Sự kiện
    List<AwardResponse> getAwardsByEvent(Long eventId);

    // Lấy loại giải thưởng
    List<Map<String, Object>> getAwardTypes();

    // Lấy categories của event
    List<Map<String, Object>> getCategoriesByEvent(Long eventId);

    // Lấy team đạt điều kiện nhận
    @Transactional(readOnly = true)
    List<Map<String, Object>> getEligibleTeamsForAward(Long eventId, Long categoryId);

    // Công bố kết quả event
    @Transactional
    void publishEventResults(Long eventId, Long userId);

    // Gợi ý awards cho từng team
    List<Map<String, Object>> getSuggestedAwards(Long eventId, Long categoryId);
}