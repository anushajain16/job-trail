package com.example.anusha.job_trail.status.dto;

import com.example.anusha.job_trail.status.Stage;
import jakarta.validation.constraints.NotNull;

public record StageChangeRequest(@NotNull Stage stage) {
}
