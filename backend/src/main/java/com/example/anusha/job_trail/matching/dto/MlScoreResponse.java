package com.example.anusha.job_trail.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Deserialization target for ml-service's {@code POST /score} response body. */
public record MlScoreResponse(
        @JsonProperty("match_pct") double matchPct,
        @JsonProperty("matched_skills") List<String> matchedSkills,
        @JsonProperty("missing_skills") List<String> missingSkills,
        @JsonProperty("considered_skills") List<String> consideredSkills
) {
}
