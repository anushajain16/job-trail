package com.example.anusha.job_trail.analytics.dto;

import java.util.List;

public record TimeInStageResponse(
        List<StageDuration> stages
) {
}
