package com.example.anusha.job_trail.analytics.dto;

import java.util.List;

/**
 * Funnel counts in pipeline order (SAVED through OFFER). REJECTED and
 * GHOSTED are exits, not funnel stages, so they don't appear here — see
 * {@link com.example.anusha.job_trail.analytics.dto.SourceResponseRate} for
 * where that outcome shows up instead.
 */
public record FunnelResponse(
        long totalApplications,
        List<FunnelStageCount> stages
) {
}
