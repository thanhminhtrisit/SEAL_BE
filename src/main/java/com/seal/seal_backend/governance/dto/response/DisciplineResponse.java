package com.seal.seal_backend.governance.dto.response;

import com.seal.seal_backend.domain.entity.Discipline;

public record DisciplineResponse(Long id, String code, String name, String description) {
    public static DisciplineResponse from(Discipline d) {
        return new DisciplineResponse(d.getId(), d.getCode(), d.getName(), d.getDescription());
    }
}
