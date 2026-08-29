package com.example.anusha.job_trail.googlecalendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body of Google's {@code POST https://oauth2.googleapis.com/token}
 * — shared shape for both the authorization-code exchange and the
 * refresh-token grant. {@code refreshToken} is only ever present on the
 * first (a refresh-token grant issues a new access token, not a new
 * refresh token — Google keeps handing back the same one, or none at all).
 */
public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresInSeconds,
        String scope
) {
}
