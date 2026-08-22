package com.example.anusha.job_trail.auth.oauth;

import com.example.anusha.job_trail.auth.exception.OAuthVerificationException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * GitHub has no ID token, so verification is two real HTTP calls rather
 * than local signature checking: exchange the authorization code the
 * frontend obtained (via GitHub's own redirect flow) for an access token,
 * then use that token to fetch the profile it belongs to. Both calls
 * authenticate the request server-side with the app's client secret, which
 * is what makes the exchange trustworthy — a code alone proves nothing.
 */
@Component
public class GitHubOAuthClient implements OAuthProviderClient {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    // Built directly rather than injecting a RestClient.Builder bean: this
    // app doesn't otherwise pull in Boot's HTTP client autoconfiguration
    // (no other outbound REST calls exist), so there's no such bean to
    // autowire — a plain RestClient.create() needs nothing from the context.
    private final RestClient restClient = RestClient.create();
    private final OAuthProperties properties;

    public GitHubOAuthClient(OAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GITHUB;
    }

    @Override
    public OAuthUserInfo resolve(String code) {
        String accessToken = exchangeCodeForAccessToken(code);
        GitHubUserResponse profile = fetchProfile(accessToken);
        String email = profile.email() != null ? profile.email() : fetchPrimaryVerifiedEmail(accessToken);
        if (email == null) {
            throw new OAuthVerificationException("github");
        }
        return new OAuthUserInfo(String.valueOf(profile.id()), email);
    }

    private String exchangeCodeForAccessToken(String code) {
        try {
            GitHubTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .header("Accept", "application/json")
                    .body(Map.of(
                            "client_id", properties.github().clientId(),
                            "client_secret", properties.github().clientSecret(),
                            "code", code))
                    .retrieve()
                    .body(GitHubTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new OAuthVerificationException("github");
            }
            return response.accessToken();
        } catch (RestClientException e) {
            throw new OAuthVerificationException("github");
        }
    }

    private GitHubUserResponse fetchProfile(String accessToken) {
        try {
            GitHubUserResponse profile = restClient.get()
                    .uri(USER_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubUserResponse.class);
            if (profile == null) {
                throw new OAuthVerificationException("github");
            }
            return profile;
        } catch (RestClientException e) {
            throw new OAuthVerificationException("github");
        }
    }

    private String fetchPrimaryVerifiedEmail(String accessToken) {
        try {
            List<GitHubEmailResponse> emails = restClient.get()
                    .uri(EMAILS_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubEmailResponse>>() {
                    });
            if (emails == null) {
                return null;
            }
            return emails.stream()
                    .filter(GitHubEmailResponse::primary)
                    .filter(GitHubEmailResponse::verified)
                    .map(GitHubEmailResponse::email)
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException e) {
            return null;
        }
    }
}
