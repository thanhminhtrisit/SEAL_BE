package com.seal.seal_backend.ranking.dto.response;

public record RankingResponse (
    Long rankingId,
    Long teamId,
    String teamName,
    Long roundId,
    Double totalScore,
    Integer rankPosition,
    Boolean isPromoted
){

}
