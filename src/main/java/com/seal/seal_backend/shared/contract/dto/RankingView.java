package com.seal.seal_backend.shared.contract.dto;

import java.math.BigDecimal;

public record RankingView(Long teamId, Long roundId, Long categoryId,
                          BigDecimal totalScore, int rankPosition, boolean promoted) {}
