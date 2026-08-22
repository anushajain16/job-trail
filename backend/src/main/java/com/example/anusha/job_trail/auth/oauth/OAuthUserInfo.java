package com.example.anusha.job_trail.auth.oauth;

/**
 * What a provider client extracts after successfully verifying a client-supplied
 * token: a stable subject id and the account's email. Nothing else from the
 * provider's profile is needed to authenticate.
 */
public record OAuthUserInfo(String subject, String email) {
}
