package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.googlecalendar.dto.GoogleTokenResponse;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarUnavailableException;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Owns the google_connection table: turning an authorization code into a
 * stored (encrypted) refresh token, minting a fresh access token from it on
 * demand, and disconnecting. There's no access-token cache here — every
 * {@link #getValidAccessToken} call spends one refresh-token-grant round
 * trip to Google — calendar-sync is an explicit, low-frequency button
 * click, not a hot path, so the simplicity of "always refresh" outweighs
 * the complexity of tracking an access token's own expiry.
 */
@Service
public class GoogleConnectionService {

    private final GoogleConnectionRepository googleConnectionRepository;
    private final UserRepository userRepository;
    private final GoogleOAuthTokenClient googleOAuthTokenClient;
    private final TokenCipher tokenCipher;

    public GoogleConnectionService(GoogleConnectionRepository googleConnectionRepository, UserRepository userRepository,
                                    GoogleOAuthTokenClient googleOAuthTokenClient, TokenCipher tokenCipher) {
        this.googleConnectionRepository = googleConnectionRepository;
        this.userRepository = userRepository;
        this.googleOAuthTokenClient = googleOAuthTokenClient;
        this.tokenCipher = tokenCipher;
    }

    /**
     * Exchanges the authorization code the callback received for tokens
     * and stores the refresh token (encrypted). Google only issues a
     * refresh token on the grant that first asks for offline access —
     * {@link GoogleCalendarController} always requests {@code prompt=consent}
     * precisely so this path can count on getting one on every fresh
     * connect; a reconnect where Google nonetheless omits one keeps
     * whatever credential is already on file rather than erroring.
     */
    @Transactional
    public void connect(UUID userId, String code) {
        GoogleTokenResponse tokenResponse = googleOAuthTokenClient.exchangeCode(code);
        Optional<GoogleConnection> existing = googleConnectionRepository.findByUserId(userId);

        String refreshToken = tokenResponse.refreshToken();
        if (refreshToken == null) {
            if (existing.isEmpty()) {
                throw new GoogleCalendarUnavailableException(
                        "Google did not return a refresh token; disconnect any prior grant and try connecting again");
            }
            return; // Already-stored credential is still good; nothing to update.
        }

        String encrypted = tokenCipher.encrypt(refreshToken);
        if (existing.isPresent()) {
            existing.get().reconnect(encrypted, tokenResponse.scope());
        } else {
            User userRef = userRepository.getReferenceById(userId);
            googleConnectionRepository.save(new GoogleConnection(userRef, encrypted, tokenResponse.scope()));
        }
    }

    /** Empty means "not connected" — the caller (CalendarSyncService)
     * turns that into {@link com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarNotConnectedException}. */
    @Transactional(readOnly = true)
    public Optional<String> getValidAccessToken(UUID userId) {
        return googleConnectionRepository.findByUserId(userId)
                .map(connection -> googleOAuthTokenClient.refresh(tokenCipher.decrypt(connection.getEncryptedRefreshToken())))
                .map(GoogleTokenResponse::accessToken);
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return googleConnectionRepository.findByUserId(userId).isPresent();
    }

    /** Deletes the stored credential outright — every future sync attempt
     * then fails fast with GoogleCalendarNotConnectedException instead of
     * silently using a stale/revoked token. */
    @Transactional
    public void disconnect(UUID userId) {
        googleConnectionRepository.deleteByUserId(userId);
    }
}
