package com.example.anusha.job_trail.interview.dto;

import java.time.Instant;
import java.util.UUID;

public record InterviewRoundResponse(
        UUID id,
        UUID applicationId,
        String roundType,
        Instant scheduledAt,
        String interviewerName,
        String questionsAsked,
        String notes,
        String reflection,
        Instant createdAt,
        Instant updatedAt
) {
}
