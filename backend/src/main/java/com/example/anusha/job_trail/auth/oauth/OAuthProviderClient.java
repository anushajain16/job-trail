package com.example.anusha.job_trail.auth.oauth;

/**
 * One implementation per external provider. {@link #resolve} takes whatever
 * token/code shape that provider hands the frontend and turns it into a
 * verified {@link OAuthUserInfo} — or throws {@link OAuthVerificationException}
 * if it can't be verified. Implementations never trust the token's claims
 * without checking them against the provider (signature verification for
 * Google's JWT, an actual API call for GitHub's opaque code).
 */
public interface OAuthProviderClient {

    AuthProvider provider();

    OAuthUserInfo resolve(String token);
}
