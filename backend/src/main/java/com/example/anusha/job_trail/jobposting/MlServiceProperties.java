package com.example.anusha.job_trail.jobposting;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code app.ml-service.*}. This is the only outbound call the app
 * makes to M1 (the FastAPI parse/match service) — every value here exists
 * to make that call fail fast and predictably rather than hang the request
 * thread, since {@link MlServiceParseClient} treats any failure as a signal
 * to fall back to manual entry, never as something to surface as a 500.
 *
 * @param baseUrl        where the ml-service lives. Defaults to the Docker
 *                        Compose service name/port ({@code ml-service:8000}),
 *                        i.e. reachable only over the internal container
 *                        network — this is never meant to be a public URL,
 *                        and nothing here does TLS/auth hardening for one.
 * @param sharedSecret   sent as the {@code X-Internal-Api-Key} header on
 *                        every request; ml-service checks it when its own
 *                        {@code MLSVC_INTERNAL_API_KEY} is set. Blank in
 *                        local dev (both sides default to "no check"), but
 *                        every real deploy should set the same value on
 *                        both services.
 * @param connectTimeout  how long to wait for the TCP handshake.
 * @param readTimeout     how long to wait for the response body once
 *                        connected — the ml-service's own scrape+LLM budget
 *                        can be tens of seconds, so this must exceed it.
 * @param maxRetries     additional attempts after the first, only for
 *                        connection failures/timeouts and 5xx responses
 *                        (never for a 4xx — retrying a bad request just
 *                        wastes the retry budget).
 * @param retryBackoff   delay before each retry attempt.
 */
@ConfigurationProperties(prefix = "app.ml-service")
public record MlServiceProperties(
        String baseUrl,
        String sharedSecret,
        Duration connectTimeout,
        Duration readTimeout,
        int maxRetries,
        Duration retryBackoff
) {
}
