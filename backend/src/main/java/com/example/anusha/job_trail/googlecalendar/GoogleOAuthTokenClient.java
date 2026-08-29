package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.googlecalendar.dto.GoogleTokenResponse;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * The two calls this app makes to Google's OAuth2 token endpoint: the
 * one-time authorization-code exchange (right after the user grants
 * consent) and the ongoing refresh-token grant (every time a valid access
 * token is needed for a Calendar API call — see
 * {@link GoogleConnectionService#getValidAccessToken}). Same "plain
 * RestClient, no SDK flow classes" shape as {@code GitHubOAuthClient}
 * uses for its own token exchange.
 */
@Component
public class GoogleOAuthTokenClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final RestClient restClient = RestClient.create();
    private final GoogleCalendarProperties properties;

    public GoogleOAuthTokenClient(GoogleCalendarProperties properties) {
        this.properties = properties;
    }

    public GoogleTokenResponse exchangeCode(String code) {
        return post(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret(),
                "redirect_uri", properties.redirectUri()
        ));
    }

    public GoogleTokenResponse refresh(String refreshToken) {
        return post(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret()
        ));
    }

    // Google's token endpoint requires application/x-www-form-urlencoded
    // specifically — a JSON body is rejected — hence the explicit content
    // type and MultiValueMap rather than a plain Map.
    private GoogleTokenResponse post(Map<String, String> form) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        form.forEach(body::add);
        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new GoogleCalendarUnavailableException("Google's token endpoint returned an empty response");
            }
            return response;
        } catch (RestClientException e) {
            throw new GoogleCalendarUnavailableException("Failed to reach Google's token endpoint", e);
        }
    }
}
