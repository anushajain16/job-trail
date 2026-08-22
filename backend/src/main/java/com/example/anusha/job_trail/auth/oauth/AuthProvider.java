package com.example.anusha.job_trail.auth.oauth;

/**
 * External identity providers a user can sign in with, in addition to (or
 * instead of) a local password. The name is persisted as-is in
 * {@code user_identities.provider} and appears in the {@code /api/auth/oauth/{provider}}
 * path, so renaming a constant is a breaking change.
 */
public enum AuthProvider {
    GOOGLE,
    GITHUB
}
