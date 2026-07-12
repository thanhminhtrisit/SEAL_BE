package com.seal.seal_backend.scoring.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EvaluationHistoryResponse {

    private String evaluationStatus;
    private List<EvaluationHistoryItemResponse> items;
}
