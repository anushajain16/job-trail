package com.example.anusha.job_trail.analytics.dto;

import com.example.anusha.job_trail.status.Stage;

/**
 * How many of the caller's applications ever reached {@code stage} — an
 * application that skipped straight from SAVED to OFFER still counts at
 * every stage in between as far as this number is concerned, since the
 * funnel counts "reached", not "passed through in order".
 */
public record FunnelStageCount(
        Stage stage,
        long applications
) {
}
