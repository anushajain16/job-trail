package com.example.anusha.job_trail.matching.dto;

/** Deserialization target for ml-service's {@code POST /profile} response body. */
public record MlProfileResponse(
        MlResumeProfile profile,
        double confidence
) {
}
