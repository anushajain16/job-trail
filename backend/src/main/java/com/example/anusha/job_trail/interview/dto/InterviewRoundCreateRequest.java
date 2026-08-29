package com.example.anusha.job_trail.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record InterviewRoundCreateRequest(
        @NotBlank @Size(max = 100) String roundType,
        Instant scheduledAt,
        @Size(max = 255) String interviewerName,
        String questionsAsked,
        @Size(max = 5000) String notes,
        String reflection
) {
}
