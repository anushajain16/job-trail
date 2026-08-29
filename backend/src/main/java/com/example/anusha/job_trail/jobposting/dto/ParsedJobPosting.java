package com.example.anusha.job_trail.jobposting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mirrors ml-service's {@code ParsedJobPosting} schema (see
 * {@code ml-service/app/schemas.py}). Every field is nullable/best-effort —
 * this is what {@code /parse} extracted, not a validated {@code Application}.
 * ml-service is FastAPI/Pydantic and returns snake_case field names, hence
 * the explicit {@link JsonProperty} mapping to this side's camelCase.
 */
public record ParsedJobPosting(
        String company,
        String role,
        String location,
        @JsonProperty("employment_type") String employmentType,
        String seniority,
        @JsonProperty("salary_min") Double salaryMin,
        @JsonProperty("salary_max") Double salaryMax,
        String currency,
        @JsonProperty("required_skills") List<String> requiredSkills,
        @JsonProperty("nice_to_have_skills") List<String> niceToHaveSkills,
        String summary
) {
}
