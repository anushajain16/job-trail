package com.example.anusha.job_trail.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body for ml-service's {@code POST /score}. {@code requiredSkills}
 * is always sent null — this app never has an LLM-derived required-skills
 * list handed to it the way ParsedJobPosting would provide one, so /score
 * always falls back to its own vocabulary-derived list from the JD text. */
public record MlScoreRequest(
        MlResumeProfile profile,
        @JsonProperty("job_description_text") String jobDescriptionText
) {
}
