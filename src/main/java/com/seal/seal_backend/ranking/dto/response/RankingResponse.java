package com.seal.seal_backend.ranking.dto.response;

import com.seal.seal_backend.domain.entity.Category;

public record RankingResponse (
    Long rankingId,
    Long teamId,
    String teamName,
    String categoryName,
    Long roundId,
    Double totalScore,
    Integer rankPosition,
    Boolean isPromoted
){

}
