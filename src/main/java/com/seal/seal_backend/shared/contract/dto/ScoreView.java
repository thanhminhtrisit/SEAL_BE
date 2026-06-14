package com.seal.seal_backend.shared.contract.dto;

import java.math.BigDecimal;

/** A single per-judge per-criterion score (BR-SCR-02: never merged at input). */
public record ScoreView(Long evaluationId, Long judgeId, Long submissionVersionId,
                        Long criterionId, BigDecimal scoreValue) {}
