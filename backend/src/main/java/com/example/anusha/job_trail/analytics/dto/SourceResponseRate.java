package com.example.anusha.job_trail.analytics.dto;

/**
 * "Responded" means the employer ever gave a signal back — the application's
 * history reached SCREEN, INTERVIEW, FINAL, OFFER, or REJECTED at some
 * point. GHOSTED is the one outcome that does *not* count as a response —
 * that's the whole reason it's a distinct stage from REJECTED. Applications
 * with no {@code source} on the parent record are grouped under "Unknown".
 */
public record SourceResponseRate(
        String source,
        long totalApplications,
        long respondedApplications,
        double responseRate
) {
}
