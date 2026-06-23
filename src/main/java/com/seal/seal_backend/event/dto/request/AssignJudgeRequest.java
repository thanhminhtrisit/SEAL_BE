package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignJudgeRequest(
        @NotNull Long judgeId,
        Long categoryId
) {}
