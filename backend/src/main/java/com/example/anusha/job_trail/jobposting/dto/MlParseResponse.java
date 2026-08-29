package com.example.anusha.job_trail.jobposting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deserialization target for ml-service's {@code POST /parse} response body
 * (its {@code ParseResponse} schema). Internal to the client — callers see
 * {@link ParseUrlResponse}, not this.
 */
public record MlParseResponse(
        String source,
        @JsonProperty("source_url") String sourceUrl,
        ParsedJobPosting parsed,
        double confidence
) {
}
