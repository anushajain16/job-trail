package com.example.anusha.job_trail.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code app.documents.*}. {@code maxSizeBytes} and the upload type
 * whitelist are enforced by {@link DocumentService} before anything touches
 * storage; {@code storage.*} configures the MinIO/S3 client itself.
 */
@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(
        long maxSizeBytes,
        Storage storage
) {

    public record Storage(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            Duration presignedUrlTtl
    ) {
    }
}
