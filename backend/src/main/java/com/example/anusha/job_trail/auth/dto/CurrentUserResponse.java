package com.example.anusha.job_trail.auth.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String email) {
}
