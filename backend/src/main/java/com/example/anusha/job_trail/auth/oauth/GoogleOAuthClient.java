package com.example.anusha.job_trail.auth.oauth;

import com.example.anusha.job_trail.auth.exception.OAuthVerificationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifies the ID token Google Identity Services hands the frontend
 * directly — no server-side call to Google needed, the token is a signed
 * JWT and {@link GoogleIdTokenVerifier} checks its signature, expiry, and
 * that it was issued for this app (the audience check), all locally.
 */
@Component
public class GoogleOAuthClient implements OAuthProviderClient {

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthClient(OAuthProperties properties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(properties.google().clientId()))
                .build();
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo resolve(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            // Malformed token, network failure fetching Google's signing
            // keys, whatever the cause — surfaces to the caller the same as
            // a token that verified false: not usable.
            throw new OAuthVerificationException("google");
        }
        if (idToken == null) {
            throw new OAuthVerificationException("google");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        return new OAuthUserInfo(payload.getSubject(), payload.getEmail());
    }
}
