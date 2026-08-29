package com.example.anusha.job_trail.matching;

import com.example.anusha.job_trail.jobposting.MlServiceProperties;
import com.example.anusha.job_trail.matching.dto.MlProfileRequest;
import com.example.anusha.job_trail.matching.dto.MlProfileResponse;
import com.example.anusha.job_trail.matching.dto.MlResumeProfile;
import com.example.anusha.job_trail.matching.dto.MlScoreRequest;
import com.example.anusha.job_trail.matching.dto.MlScoreResponse;
import com.example.anusha.job_trail.matching.exception.MlServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * The one place this app talks to ml-service's {@code POST /profile} and
 * {@code POST /score} — the M1-extension counterpart to
 * {@code jobposting.MlServiceParseClient}, reusing its connection settings
 * ({@link MlServiceProperties} — one {@code app.ml-service.*} config for
 * every feature that calls this service, not one per feature) and the same
 * retry shape: transient failures (timeout, connection error, 5xx) retry up
 * to {@code max-retries} times; a 4xx fails immediately since retrying sends
 * the identical request.
 *
 * <p>Unlike the parse client, a failure here always surfaces as
 * {@link MlServiceUnavailableException} for the caller to let escape as a
 * 502 — {@code ResumeProfileService}/{@code MatchScoringService} don't
 * catch it, since there's no graceful "fall back to manual entry" for a
 * match score the way there is for autofilling a form.
 */
@Component
public class MlServiceMatchClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceMatchClient.class);

    private final RestClient restClient;
    private final MlServiceProperties properties;

    public MlServiceMatchClient(RestClient mlServiceRestClient, MlServiceProperties properties) {
        this.restClient = mlServiceRestClient;
        this.properties = properties;
    }

    public MlProfileResponse parseProfile(String resumeText) {
        MlProfileResponse response = callWithRetry("/profile", new MlProfileRequest(resumeText), MlProfileResponse.class);
        if (response.profile() == null) {
            throw new MlServiceUnavailableException("ml-service /profile returned an empty response");
        }
        return response;
    }

    public MlScoreResponse score(MlResumeProfile profile, String jobDescriptionText) {
        MlScoreResponse response = callWithRetry(
                "/score", new MlScoreRequest(profile, jobDescriptionText), MlScoreResponse.class);
        if (response.matchedSkills() == null || response.missingSkills() == null) {
            throw new MlServiceUnavailableException("ml-service /score returned an empty response");
        }
        return response;
    }

    private <T> T callWithRetry(String path, Object requestBody, Class<T> responseType) {
        int attempt = 0;
        RuntimeException lastFailure;
        while (true) {
            try {
                T response = restClient.post()
                        .uri(path)
                        .body(requestBody)
                        .retrieve()
                        .body(responseType);
                if (response == null) {
                    throw new MlServiceUnavailableException("ml-service " + path + " returned an empty response");
                }
                return response;
            } catch (HttpServerErrorException e) {
                lastFailure = e; // 5xx: transient on their end, worth retrying
            } catch (RestClientResponseException e) {
                // 4xx: the request itself was rejected — retrying sends the
                // exact same request, so don't bother.
                throw new MlServiceUnavailableException("ml-service rejected the request: " + e.getStatusCode(), e);
            } catch (RestClientException e) {
                lastFailure = e; // connect refused, timeout, DNS, etc. — transient
            }

            attempt++;
            if (attempt > properties.maxRetries()) {
                throw new MlServiceUnavailableException(
                        "ml-service " + path + " failed after " + attempt + " attempt(s)", lastFailure);
            }
            log.warn("ml-service {} attempt {} failed, retrying: {}", path, attempt, lastFailure.getMessage());
            sleep(properties.retryBackoff());
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MlServiceUnavailableException("Interrupted while retrying ml-service call", e);
        }
    }
}
