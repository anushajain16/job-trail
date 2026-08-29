package com.example.anusha.job_trail.application.dto;

import com.example.anusha.job_trail.status.Stage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
        String jobDescriptionText,
        LocalDate deadline,
        Stage currentStage,
        UUID resumeVersionId,
        UUID coverLetterVersionId,
        // Null until POST /{id}/score has run at least once — see
        // matching.MatchScoringService. matchedSkills/missingSkills are
        // empty lists (never null) once a score exists.
        Double matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        Instant scoredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
