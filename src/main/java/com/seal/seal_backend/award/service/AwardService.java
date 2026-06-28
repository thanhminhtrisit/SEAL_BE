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

    @Transactional(readOnly = true)
    List<Map<String, Object>> getEligibleTeamsForAward(Long eventId);

    @Transactional
    void publishEventResults(Long eventId, Long userId);
}