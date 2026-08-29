package com.example.anusha.job_trail.jobposting.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * Doubles as ml-service's wire shape (nested inside {@link MlParseResponse}
 * to deserialize its snake_case {@code POST /parse} response) and this
 * app's own outbound shape (nested inside {@link ParseUrlResponse}, the
 * frontend-facing DTO) — so its Jackson names have to work in both
 * directions at once: {@link JsonAlias} accepts ml-service's snake_case
 * names on the way in, without changing what gets serialized on the way
 * out, which stays this side's plain camelCase (unlike {@code @JsonProperty},
 * which would apply the same name to both directions and leak
 * ml-service's naming into this app's own API).
 */
public record ParsedJobPosting(
        String company,
        String role,
        String location,
        @JsonAlias("employment_type") String employmentType,
        String seniority,
        @JsonAlias("salary_min") Double salaryMin,
        @JsonAlias("salary_max") Double salaryMax,
        String currency,
        @JsonAlias("required_skills") List<String> requiredSkills,
        @JsonAlias("nice_to_have_skills") List<String> niceToHaveSkills,
        String summary
) {
}
