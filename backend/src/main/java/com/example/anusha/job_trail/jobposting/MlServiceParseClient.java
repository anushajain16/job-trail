package com.example.anusha.job_trail.jobposting;

import com.example.anusha.job_trail.jobposting.dto.MlParseResponse;
import com.example.anusha.job_trail.jobposting.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * The one place this app talks to ml-service's {@code POST /parse}. Retries
 * only what retrying can fix — a connection failure/timeout or a 5xx — up
 * to {@code app.ml-service.max-retries} extra attempts with a fixed delay
 * between them; a 4xx (malformed URL, ml-service rejected it) fails
 * immediately since trying again changes nothing.
 *
 * <p>Every failure path, retried-out or not, surfaces as
 * {@link MlServiceUnavailableException} — {@link JobPostingParseService} is
 * what turns that into the graceful "fall back to manual entry" response.
 */
@Component
public class MlServiceParseClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceParseClient.class);

    private final RestClient restClient;
    private final MlServiceProperties properties;

    public MlServiceParseClient(RestClient mlServiceRestClient, MlServiceProperties properties) {
        this.restClient = mlServiceRestClient;
        this.properties = properties;
    }

    public MlParseResponse parseUrl(String url) {
        int attempt = 0;
        RuntimeException lastFailure;
        while (true) {
            try {
                MlParseResponse response = restClient.post()
                        .uri("/parse")
                        .body(Map.of("url", url))
                        .retrieve()
                        .body(MlParseResponse.class);
                if (response == null || response.parsed() == null) {
                    throw new MlServiceUnavailableException("ml-service /parse returned an empty response");
                }
                return response;
            } catch (HttpServerErrorException e) {
                lastFailure = e; // 5xx: transient on their end, worth retrying
            } catch (RestClientResponseException e) {
                // 4xx: the request itself was rejected (bad/unscrapeable URL) —
                // retrying sends the exact same request, so don't bother.
                throw new MlServiceUnavailableException("ml-service rejected the request: " + e.getStatusCode(), e);
            } catch (RestClientException e) {
                lastFailure = e; // connect refused, timeout, DNS, etc. — transient
            }

            attempt++;
            if (attempt > properties.maxRetries()) {
                throw new MlServiceUnavailableException(
                        "ml-service /parse failed after " + attempt + " attempt(s)", lastFailure);
            }
            log.warn("ml-service /parse attempt {} failed, retrying: {}", attempt, lastFailure.getMessage());
            sleep(properties.retryBackoff());
        }
    }

    private void sleep(java.time.Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MlServiceUnavailableException("Interrupted while retrying ml-service call", e);
        }
    }
}
