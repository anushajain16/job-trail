package com.example.anusha.job_trail.analytics.dto;

import com.example.anusha.job_trail.status.Stage;

/**
 * Average time (in fractional days) spent in {@code stage} before moving on,
 * averaged over {@code sampleSize} completed transitions out of that stage.
 * An application still sitting in a stage — no next history row yet —
 * contributes no sample; there's no "elapsed so far" fudge here, only
 * intervals that actually closed.
 */
public record StageDuration(
        Stage stage,
        double averageDays,
        long sampleSize
) {
}
