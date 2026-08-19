package com.example.anusha.job_trail.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        String location,
        Integer salaryMin,
        Integer salaryMax,
        String link,
        String source,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
