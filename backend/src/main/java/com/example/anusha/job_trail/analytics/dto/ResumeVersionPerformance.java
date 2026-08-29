package com.example.anusha.job_trail.analytics.dto;

import java.util.UUID;

/**
 * Response rate for one named resume version, over every application it was
 * attached to. "Responded" uses the same definition as
 * {@link SourceResponseRate}: the application's history ever reached SCREEN,
 * INTERVIEW, FINAL, OFFER, or REJECTED. A version nobody has attached to an
 * application yet still appears here, with zero counts and a zero rate.
 */
public record ResumeVersionPerformance(
        UUID documentId,
        String label,
        long totalApplications,
        long respondedApplications,
        double responseRate
) {
}
