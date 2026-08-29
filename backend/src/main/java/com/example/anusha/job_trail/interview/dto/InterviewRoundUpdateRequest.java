package com.example.anusha.job_trail.interview.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PATCH body: every field is optional, and a field left out of the
 * request (null here) leaves the stored value untouched — same
 * null-means-unchanged semantics as
 * {@link com.example.anusha.job_trail.application.dto.ApplicationUpdateRequest}.
 * There's no way to null out a field via this endpoint either.
 */
public record InterviewRoundUpdateRequest(
        @Size(max = 100) String roundType,
        Instant scheduledAt,
        @Size(max = 255) String interviewerName,
        String questionsAsked,
        @Size(max = 5000) String notes,
        String reflection
) {
}
