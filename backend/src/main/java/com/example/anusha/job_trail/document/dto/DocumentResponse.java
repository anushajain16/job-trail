package com.example.anusha.job_trail.document.dto;

import com.example.anusha.job_trail.document.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentType type,
        String label,
        String originalFilename,
        String contentType,
        long size,
        Instant uploadedAt
) {
}
