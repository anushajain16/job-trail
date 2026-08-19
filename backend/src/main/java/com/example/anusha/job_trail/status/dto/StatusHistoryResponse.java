package com.example.anusha.job_trail.status.dto;

import com.example.anusha.job_trail.status.Stage;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        Stage stage,
        Instant changedAt
) {
}
