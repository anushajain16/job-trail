package com.example.anusha.job_trail.analytics.dto;

import java.util.List;

public record ConversionResponse(
        List<StageConversion> stageConversions,
        List<SourceResponseRate> responseRateBySource
) {
}
