package com.seal.seal_backend.award.dto.request;


import com.seal.seal_backend.domain.enums.AwardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AwardCreateRequest(
        @NotNull(message = "Event ID không được để trống")
        Long eventId,

        @NotNull(message = "Team ID không được để trống")
        Long teamId,

        Long rankingId, // Có thể null nếu trao giải phụ không xét hạng

        @NotBlank(message = "Loại giải thưởng không được để trống")
        AwardType awardType, // VD: "FIRST_PLACE", "BEST_TECHNICAL"

        String description // VD: "Phần thưởng trị giá 10 triệu đồng"
) {
}