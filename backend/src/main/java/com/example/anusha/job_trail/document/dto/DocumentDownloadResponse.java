package com.example.anusha.job_trail.document.dto;

import java.time.Instant;

/**
 * A time-limited link to the actual bytes, not the bytes themselves — the
 * client downloads directly from object storage using {@code downloadUrl},
 * which stops working after {@code expiresAt}.
 */
public record DocumentDownloadResponse(
        String downloadUrl,
        String filename,
        String contentType,
        Instant expiresAt
) {
}
