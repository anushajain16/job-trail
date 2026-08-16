package com.example.anusha.job_trail.auth.security;

import java.util.UUID;

/**
 * The authenticated principal for the lifetime of a request. Carries only
 * what's in the access token's claims — deliberately not a DB lookup, so
 * validating a request never costs a query. If a handler needs the full
 * {@code User} row it can load it itself via {@link #id()}.
 */
public record AuthenticatedUser(UUID id, String email) {
}
