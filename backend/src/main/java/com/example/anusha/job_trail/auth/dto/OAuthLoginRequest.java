package com.example.anusha.job_trail.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code token} is provider-shaped, not a uniform concept: for Google it's
 * the ID token Google Identity Services returns to the frontend directly;
 * for GitHub it's the authorization {@code code} from GitHub's redirect
 * flow, which the backend still has to exchange for an access token itself.
 */
public record OAuthLoginRequest(
        @NotBlank String token
) {
}
