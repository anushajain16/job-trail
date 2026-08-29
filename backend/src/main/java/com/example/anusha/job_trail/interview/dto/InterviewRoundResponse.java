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
        // Null until POST /{id}/calendar-sync has run at least once — see
        // googlecalendar.CalendarSyncService.
        String googleEventId,
        Instant createdAt,
        Instant updatedAt
) {
}
