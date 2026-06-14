package com.seal.seal_backend.shared.contract.dto;

import java.math.BigDecimal;

public record CriterionView(Long id, Long criteriaSetId, String name,
                            BigDecimal maxScore, BigDecimal weight, int displayOrder) {}
