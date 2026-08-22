package com.example.anusha.job_trail.auth.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.oauth.*}. No dev-only fallback like {@code JwtProperties}
 * has — there's no meaningful default client id/secret, so an environment
 * that hasn't configured these simply can't complete an OAuth login (the
 * provider client fails verification, which is the correct behavior).
 */
@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(
        Google google,
        GitHub github
) {
    public record Google(String clientId) {
    }

    public record GitHub(String clientId, String clientSecret) {
    }
}
