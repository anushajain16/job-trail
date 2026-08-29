package com.example.anusha.job_trail.analytics.dto;

import java.util.List;

/**
 * Every one of the caller's resume versions, best response rate first.
 * Applications with no resume version attached are excluded entirely — they
 * can't be attributed to any version, so they'd only distort the per-version
 * denominators.
 */
public record ResumePerformanceResponse(
        List<ResumeVersionPerformance> versions
) {
}
