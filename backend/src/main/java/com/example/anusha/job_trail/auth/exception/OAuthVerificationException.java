package com.example.anusha.job_trail.auth.exception;

/**
 * The token/code presented to {@code POST /api/auth/oauth/{provider}} could
 * not be verified with the provider — bad signature, expired, wrong
 * audience, or the provider rejected the exchange outright. Collapses every
 * such failure to one outcome (401) so nothing about *why* it failed leaks
 * to the caller.
 */
public class OAuthVerificationException extends RuntimeException {

    public OAuthVerificationException(String provider) {
        super("Could not verify credentials with " + provider);
    }
}
