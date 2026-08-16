package com.example.anusha.job_trail.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code app.jwt.*}. The default secret in application.yml is only
 * long enough to satisfy HS256's minimum key-size check and is fine for
 * local dev; every real environment must set {@code JWT_SECRET} itself.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
