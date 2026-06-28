package com.seal.seal_backend.event.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReplaceCriteriaRequest(
        @NotEmpty List<@Valid AddCriterionRequest> criteria
) {}
