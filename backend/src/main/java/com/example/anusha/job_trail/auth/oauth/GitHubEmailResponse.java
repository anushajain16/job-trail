package com.example.anusha.job_trail.auth.oauth;

/** One entry of GitHub's {@code GET /user/emails}. */
record GitHubEmailResponse(
        String email,
        boolean primary,
        boolean verified
) {
}
