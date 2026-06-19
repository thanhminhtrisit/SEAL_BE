package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.ScoringCriterion;
import java.math.BigDecimal;

public record CriterionResponse(
        Long id,
        Long criteriaSetId,
        String name,
        String description,
        BigDecimal maxScore,
        BigDecimal weight,
        Integer displayOrder,
        boolean active
) {
    public static CriterionResponse from(ScoringCriterion c) {
        return new CriterionResponse(
                c.getId(), c.getCriteriaSet().getId(), c.getName(), c.getDescription(),
                c.getMaxScore(), c.getWeight(), c.getDisplayOrder(), c.getIsActive()
        );
    }
}
