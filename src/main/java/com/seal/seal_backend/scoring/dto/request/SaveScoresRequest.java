package com.seal.seal_backend.scoring.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveScoresRequest {

    private String generalComment;

    @NotEmpty(message = "Score list must not be empty")
    private List<@Valid ScoreItemRequest> scores;
}
