package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.example.anusha.job_trail.googlecalendar.dto.CalendarEventData;
import com.example.anusha.job_trail.googlecalendar.dto.GoogleTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the real app context and a real Postgres started by
 * Testcontainers — but with every call that would otherwise reach Google
 * ({@link GoogleOAuthTokenClient}, {@link GoogleCalendarClient}) replaced
 * by a Mockito mock, per this feature's own "done when" criteria: the
 * Calendar API call is mocked in tests, nothing here hits Google.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GoogleCalendarFlowIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern STATE_PATTERN = Pattern.compile("state=([^&]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoogleCalendarProperties properties;

    @MockitoBean
    private GoogleOAuthTokenClient googleOAuthTokenClient;

    @MockitoBean
    private GoogleCalendarClient googleCalendarClient;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@jobtrail.dev";
    }

    private String newUserAccessToken() throws Exception {
        String email = uniqueEmail();
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "correct-horse-battery"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    /** Drives POST /connect (authenticated) to mint a real, validly-signed
     * state token for the given user, the same way the frontend would. */
    private String stateFor(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/google-calendar/connect").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String url = objectMapper.readTree(result.getResponse().getContentAsString()).get("authorizationUrl").asString();
        Matcher matcher = STATE_PATTERN.matcher(url);
        assertThat(matcher.find()).isTrue();
        return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
    }

    private String createApplication(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Anthropic", "role": "Backend Engineer"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private String createRound(String token, String applicationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Technical R1", "scheduledAt": "2026-09-05T15:00:00Z"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private void connectCalendar(String token) throws Exception {
        String state = stateFor(token);
        when(googleOAuthTokenClient.exchangeCode(anyString()))
                .thenReturn(new GoogleTokenResponse("access-token", "refresh-token", 3600L, "calendar.events"));
        mockMvc.perform(get("/api/google-calendar/callback").param("code", "auth-code").param("state", state))
                .andExpect(status().isFound());
        when(googleOAuthTokenClient.refresh(anyString()))
                .thenReturn(new GoogleTokenResponse("fresh-access-token", null, 3600L, "calendar.events"));
    }

    @Test
    void connect_returnsAnAuthorizationUrlRequestingTheCalendarEventsScope() throws Exception {
        String token = newUserAccessToken();

        String encodedScope = URLEncoder.encode("https://www.googleapis.com/auth/calendar.events", StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/google-calendar/connect").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl", org.hamcrest.Matchers.containsString(encodedScope)))
                .andExpect(jsonPath("$.authorizationUrl", org.hamcrest.Matchers.containsString("access_type=offline")))
                .andExpect(jsonPath("$.authorizationUrl", org.hamcrest.Matchers.containsString("prompt=consent")))
                .andExpect(jsonPath("$.authorizationUrl", org.hamcrest.Matchers.containsString("state=")));
    }

    @Test
    void connect_rejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/google-calendar/connect")).andExpect(status().isUnauthorized());
    }

    @Test
    void callback_withAValidCodeAndState_connectsAndRedirectsToFrontendSuccess() throws Exception {
        String token = newUserAccessToken();
        String state = stateFor(token);
        when(googleOAuthTokenClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleTokenResponse("access-token", "refresh-token", 3600L, "calendar.events"));

        mockMvc.perform(get("/api/google-calendar/callback").param("code", "auth-code").param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", properties.frontendRedirectUri() + "?calendarConnected=true"));

        mockMvc.perform(get("/api/google-calendar/connection").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void callback_withAnInvalidState_redirectsWithFailure_andConnectsNothing() throws Exception {
        String token = newUserAccessToken();

        mockMvc.perform(get("/api/google-calendar/callback").param("code", "auth-code").param("state", "not-a-real-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", properties.frontendRedirectUri() + "?calendarConnected=false"));

        mockMvc.perform(get("/api/google-calendar/connection").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
        verify(googleOAuthTokenClient, never()).exchangeCode(any());
    }

    @Test
    void callback_withADenialError_redirectsWithFailure_withoutExchangingAnyCode() throws Exception {
        mockMvc.perform(get("/api/google-calendar/callback").param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", properties.frontendRedirectUri() + "?calendarConnected=false"));
        verify(googleOAuthTokenClient, never()).exchangeCode(any());
    }

    @Test
    void disconnect_removesTheConnection_soFutureSyncsFailCleanlyInsteadOfUsingAStaleToken() throws Exception {
        String token = newUserAccessToken();
        connectCalendar(token);
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId);

        mockMvc.perform(delete("/api/google-calendar/connection").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/google-calendar/connection").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
        verify(googleCalendarClient, never()).insertEvent(any(), any());
    }

    @Test
    void calendarSync_createsThenUpdatesTheSameEvent_ratherThanDuplicating() throws Exception {
        String token = newUserAccessToken();
        connectCalendar(token);
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId);

        when(googleCalendarClient.insertEvent(eq("fresh-access-token"), any(CalendarEventData.class))).thenReturn("google-evt-1");

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleEventId").value("google-evt-1"));

        when(googleCalendarClient.updateEvent(eq("fresh-access-token"), eq("google-evt-1"), any(CalendarEventData.class)))
                .thenReturn("google-evt-1");

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleEventId").value("google-evt-1"));

        verify(googleCalendarClient, times(1)).insertEvent(any(), any());
        verify(googleCalendarClient, times(1)).updateEvent(any(), any(), any());
    }

    @Test
    void calendarSync_isRejected_whenNoCalendarIsConnected() throws Exception {
        String token = newUserAccessToken();
        String applicationId = createApplication(token);
        String roundId = createRound(token, applicationId);

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void calendarSync_isRejected_whenTheRoundHasNoScheduledDate() throws Exception {
        String token = newUserAccessToken();
        connectCalendar(token);
        String applicationId = createApplication(token);
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundType": "Screen"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String roundId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calendarSync_isScopedToTheCallersOwnRounds() throws Exception {
        String ownerToken = newUserAccessToken();
        String strangerToken = newUserAccessToken();
        connectCalendar(strangerToken);
        String applicationId = createApplication(ownerToken);
        String roundId = createRound(ownerToken, applicationId);

        mockMvc.perform(post("/api/interviews/" + roundId + "/calendar-sync").header("Authorization", bearer(strangerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void calendarSync_rejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/interviews/" + UUID.randomUUID() + "/calendar-sync"))
                .andExpect(status().isUnauthorized());
    }
}
