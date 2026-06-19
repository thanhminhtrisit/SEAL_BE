package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.CriteriaSet;
import com.seal.seal_backend.domain.entity.ScoringCriterion;
import java.util.List;

public record CriteriaSetResponse(
        Long id,
        String name,
        String description,
        Long eventId,
        Long roundId,
        boolean template,
        boolean defaultSet,
        List<CriterionResponse> criteria
) {
    public static CriteriaSetResponse from(CriteriaSet cs, List<ScoringCriterion> criteria) {
        return new CriteriaSetResponse(
                cs.getId(), cs.getName(), cs.getDescription(),
                cs.getEvent() != null ? cs.getEvent().getId() : null,
                cs.getRound() != null ? cs.getRound().getId() : null,
                cs.getIsTemplate(), cs.getIsDefault(),
                criteria.stream().map(CriterionResponse::from).toList()
        );
    }
}
