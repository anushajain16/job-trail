package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import com.example.anusha.job_trail.googlecalendar.dto.GoogleCalendarConnectResponse;
import com.example.anusha.job_trail.googlecalendar.dto.GoogleCalendarConnectionStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The connect flow's three endpoints, plus a status check. {@code connect}
 * and {@code connection} (GET/DELETE) are normal authenticated endpoints; only
 * {@code callback} is public (see SecurityConfig) since it's reached by a
 * full-page browser redirect from Google, not a fetch with a bearer token.
 */
@RestController
public class GoogleCalendarController {

    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";

    private final GoogleCalendarProperties properties;
    private final GoogleConnectStateService stateService;
    private final GoogleConnectionService connectionService;

    public GoogleCalendarController(GoogleCalendarProperties properties, GoogleConnectStateService stateService,
                                     GoogleConnectionService connectionService) {
        this.properties = properties;
        this.stateService = stateService;
        this.connectionService = connectionService;
    }

    @PostMapping("/api/google-calendar/connect")
    public GoogleCalendarConnectResponse connect(@CurrentUser AuthenticatedUser currentUser) {
        String state = stateService.issue(currentUser.id());
        String url = AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(properties.redirectUri())
                + "&response_type=code"
                + "&scope=" + encode(SCOPE)
                // offline (get a refresh token, not just an access token) +
                // consent (force Google to actually issue one — without
                // this, a user who granted access before gets no refresh
                // token back on a second connect).
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
        return new GoogleCalendarConnectResponse(url);
    }

    @GetMapping("/api/google-calendar/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                          @RequestParam(required = false) String state,
                                          @RequestParam(required = false) String error) {
        boolean success = false;
        if (error == null && code != null && state != null) {
            var userId = stateService.verify(state);
            if (userId.isPresent()) {
                connectionService.connect(userId.get(), code);
                success = true;
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(properties.frontendRedirectUri() + "?calendarConnected=" + success))
                .build();
    }

    @GetMapping("/api/google-calendar/connection")
    public GoogleCalendarConnectionStatusResponse connectionStatus(@CurrentUser AuthenticatedUser currentUser) {
        return new GoogleCalendarConnectionStatusResponse(connectionService.isConnected(currentUser.id()));
    }

    @DeleteMapping("/api/google-calendar/connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@CurrentUser AuthenticatedUser currentUser) {
        connectionService.disconnect(currentUser.id());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
