package com.example.anusha.job_trail.analytics.dto;

import com.example.anusha.job_trail.status.Stage;

/**
 * What fraction of applications that reached {@code fromStage} went on to
 * reach {@code toStage}. {@code conversionRate} is {@code toCount / fromCount},
 * or {@code 0.0} when {@code fromCount} is zero (nothing to convert from).
 */
public record StageConversion(
        Stage fromStage,
        Stage toStage,
        long fromCount,
        long toCount,
        double conversionRate
) {
}
