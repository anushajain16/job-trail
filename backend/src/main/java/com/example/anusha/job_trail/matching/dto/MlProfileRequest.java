package com.example.anusha.job_trail.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body for ml-service's {@code POST /profile}. */
public record MlProfileRequest(
        @JsonProperty("resume_text") String resumeText
) {
}
