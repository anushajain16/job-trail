package com.example.anusha.job_trail.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mirrors ml-service's {@code ResumeProfile} schema exactly (see
 * {@code ml-service/app/schemas.py}). Serves three roles with the same
 * shape: the {@code /profile} response body, what's persisted verbatim as
 * {@code resume_profile.profile_json}, and the {@code profile} field sent
 * on every {@code /score} request — never re-derived, since ml-service is
 * FastAPI/Pydantic and returns/expects snake_case field names, hence the
 * explicit {@link JsonProperty} mapping to this side's camelCase.
 */
public record MlResumeProfile(
        List<String> skills,
        @JsonProperty("years_experience") Double yearsExperience,
        List<String> roles,
        String seniority,
        String summary
) {
}
