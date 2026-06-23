package com.seal.seal_backend.governance.dto.response;

import com.seal.seal_backend.domain.entity.TermPlan;

public record TermPlanResponse(
        Long id,
        String term,
        Integer year,
        Long disciplineId,
        String disciplineName,
        Integer maxEvents,
        long usedEvents,
        long remaining
) {
    public static TermPlanResponse from(TermPlan tp, long usedEvents) {
        long remaining = Math.max(0, tp.getMaxEvents() - usedEvents);
        return new TermPlanResponse(
                tp.getId(),
                tp.getTerm().name(),
                tp.getYear(),
                tp.getDiscipline().getId(),
                tp.getDiscipline().getName(),
                tp.getMaxEvents(),
                usedEvents,
                remaining);
    }
}
