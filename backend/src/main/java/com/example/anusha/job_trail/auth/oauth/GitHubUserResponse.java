package com.example.anusha.job_trail.auth.oauth;

/** Response body of GitHub's {@code GET /user}. {@code email} is null when the
 * account has no public email set — {@link GitHubOAuthClient} falls back to
 * {@code GET /user/emails} in that case. */
record GitHubUserResponse(
        long id,
        String email
) {
}
