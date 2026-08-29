package com.example.anusha.job_trail.matching.dto;

import java.time.Instant;
import java.util.List;

public record MatchScoreResponse(
        double matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        Instant scoredAt,
        // True when this is a stored result reused as-is (same resume
        // profile, same JD text as last time) rather than a fresh
        // ml-service call — see MatchScoringService.
        boolean cached
) {
}
