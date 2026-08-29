package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.googlecalendar.dto.GoogleTokenResponse;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarUnavailableException;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises GoogleConnectionService's own logic (upsert-vs-create,
 * missing-refresh-token handling, disconnect) with
 * {@link GoogleOAuthTokenClient} mocked — no real call to Google.
 */
@ExtendWith(MockitoExtension.class)
class GoogleConnectionServiceTest {

    @Mock
    private GoogleConnectionRepository googleConnectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GoogleOAuthTokenClient googleOAuthTokenClient;
    @Mock
    private TokenCipher tokenCipher;

    private GoogleConnectionService googleConnectionService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        googleConnectionService = new GoogleConnectionService(googleConnectionRepository, userRepository, googleOAuthTokenClient, tokenCipher);
    }

    @Test
    void connect_savesANewConnection_whenNoneExistsYet() {
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(googleOAuthTokenClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleTokenResponse("access", "refresh-token", 3600L, "calendar.events"));
        when(tokenCipher.encrypt("refresh-token")).thenReturn("encrypted-refresh-token");
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User("owner@jobtrail.dev", "hash"));

        googleConnectionService.connect(USER_ID, "auth-code");

        verify(googleConnectionRepository).save(argThat(connection ->
                connection.getEncryptedRefreshToken().equals("encrypted-refresh-token")
                        && connection.getGrantedScopes().equals("calendar.events")));
    }

    @Test
    void connect_reconnectsInPlace_whenAConnectionAlreadyExists() {
        GoogleConnection existing = new GoogleConnection(new User("owner@jobtrail.dev", "hash"), "old-encrypted", "old-scope");
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(googleOAuthTokenClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleTokenResponse("access", "new-refresh-token", 3600L, "calendar.events"));
        when(tokenCipher.encrypt("new-refresh-token")).thenReturn("new-encrypted");

        googleConnectionService.connect(USER_ID, "auth-code");

        assertThat(existing.getEncryptedRefreshToken()).isEqualTo("new-encrypted");
        verify(googleConnectionRepository, never()).save(any());
    }

    @Test
    void connect_keepsTheExistingCredential_whenGoogleDoesNotReissueARefreshToken() {
        GoogleConnection existing = new GoogleConnection(new User("owner@jobtrail.dev", "hash"), "old-encrypted", "old-scope");
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(googleOAuthTokenClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleTokenResponse("access", null, 3600L, "calendar.events"));

        googleConnectionService.connect(USER_ID, "auth-code");

        assertThat(existing.getEncryptedRefreshToken()).isEqualTo("old-encrypted");
    }

    @Test
    void connect_throws_whenGoogleReturnsNoRefreshTokenAndThereIsNoExistingConnection() {
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(googleOAuthTokenClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleTokenResponse("access", null, 3600L, "calendar.events"));

        assertThatThrownBy(() -> googleConnectionService.connect(USER_ID, "auth-code"))
                .isInstanceOf(GoogleCalendarUnavailableException.class);
    }

    @Test
    void getValidAccessToken_decryptsAndRefreshes_whenConnected() {
        GoogleConnection connection = new GoogleConnection(new User("owner@jobtrail.dev", "hash"), "encrypted", "calendar.events");
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(connection));
        when(tokenCipher.decrypt("encrypted")).thenReturn("raw-refresh-token");
        when(googleOAuthTokenClient.refresh("raw-refresh-token"))
                .thenReturn(new GoogleTokenResponse("fresh-access-token", null, 3600L, "calendar.events"));

        Optional<String> accessToken = googleConnectionService.getValidAccessToken(USER_ID);

        assertThat(accessToken).contains("fresh-access-token");
    }

    @Test
    void getValidAccessToken_isEmpty_whenNotConnected() {
        when(googleConnectionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(googleConnectionService.getValidAccessToken(USER_ID)).isEmpty();
        verify(googleOAuthTokenClient, never()).refresh(any());
    }

    @Test
    void disconnect_deletesTheStoredConnection_soFutureSyncsStopCleanly() {
        googleConnectionService.disconnect(USER_ID);

        verify(googleConnectionRepository).deleteByUserId(USER_ID);
    }
}
