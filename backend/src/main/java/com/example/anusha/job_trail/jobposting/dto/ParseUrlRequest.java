package com.example.anusha.job_trail.jobposting.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/job-postings/parse} request body: the posting URL to scrape and extract. */
public record ParseUrlRequest(
        @NotBlank String url
) {
}
