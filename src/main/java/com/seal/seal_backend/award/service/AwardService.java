package com.seal.seal_backend.award.service;

import com.seal.seal_backend.award.dto.request.AwardCreateRequest;
import com.seal.seal_backend.award.dto.response.AwardResponse;

import java.util.List;

public interface AwardService {

    // Gán một giải thưởng cho một Team
    AwardResponse createAward(AwardCreateRequest request, Long userId);

    // Lấy danh sách giải thưởng của một Sự kiện
    List<AwardResponse> getAwardsByEvent(Long eventId);
}