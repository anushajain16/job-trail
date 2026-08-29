package com.example.anusha.job_trail.googlecalendar;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.google-calendar.*}. See application.yml's comment on
 * this block for why it's a separate OAuth client from {@code app.oauth.google}.
 *
 * @param clientId            the connect flow's OAuth client id.
 * @param clientSecret        needed to exchange an authorization code (and
 *                             to refresh an access token) — this flow, unlike
 *                             the ID-token-only login flow, is a confidential
 *                             client.
 * @param redirectUri          this app's own callback URL, registered with
 *                             the OAuth client — Google redirects the
 *                             browser here with the authorization code.
 * @param frontendRedirectUri where the callback sends the browser once it's
 *                             done, success or failure.
 * @param tokenEncryptionKey  base64-encoded 256-bit AES key used to encrypt
 *                             the one long-lived credential this app stores
 *                             at rest: the Google refresh token.
 */
@ConfigurationProperties(prefix = "app.google-calendar")
public record GoogleCalendarProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendRedirectUri,
        String tokenEncryptionKey
) {
}
