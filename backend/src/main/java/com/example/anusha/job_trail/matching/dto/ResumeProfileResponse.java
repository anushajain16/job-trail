package com.example.anusha.job_trail.matching.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeProfileResponse(
        UUID id,
        UUID sourceDocumentId,
        MlResumeProfile profile,
        double confidence,
        Instant parsedAt
) {
}
