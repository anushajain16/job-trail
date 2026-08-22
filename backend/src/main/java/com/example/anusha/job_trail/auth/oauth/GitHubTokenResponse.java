package com.example.anusha.job_trail.auth.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body of GitHub's {@code POST /login/oauth/access_token} (asked
 * for as JSON via the {@code Accept} header — its default is form-encoded).
 * On a bad/expired/already-used code, GitHub returns 200 with an
 * {@code error} field instead of a non-2xx status, so {@link GitHubOAuthClient}
 * checks {@link #accessToken()} for null rather than trusting the HTTP status.
 */
record GitHubTokenResponse(
        @JsonProperty("access_token") String accessToken
) {
}
