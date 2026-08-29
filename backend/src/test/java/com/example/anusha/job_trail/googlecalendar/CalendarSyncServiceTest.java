package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.googlecalendar.dto.CalendarEventData;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarNotConnectedException;
import com.example.anusha.job_trail.interview.InterviewRound;
import com.example.anusha.job_trail.interview.InterviewRoundMapperImpl;
import com.example.anusha.job_trail.interview.InterviewRoundRepository;
import com.example.anusha.job_trail.interview.dto.InterviewRoundResponse;
import com.example.anusha.job_trail.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises CalendarSyncService's own logic (ownership lookup, event
 * building, create-vs-update branching) with {@link GoogleCalendarClient}
 * and {@link GoogleConnectionService} mocked — no real call to Google, per
 * this feature's own "done when" criteria.
 */
@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {

    @Mock
    private InterviewRoundRepository interviewRoundRepository;
    @Mock
    private GoogleConnectionService googleConnectionService;
    @Mock
    private GoogleCalendarClient googleCalendarClient;

    private CalendarSyncService calendarSyncService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUND_ID = UUID.randomUUID();
    private static final String ACCESS_TOKEN = "fake-access-token";

    private InterviewRound newRound() {
        User user = new User("owner@jobtrail.dev", "hash");
        Application application = new Application(user, "Anthropic", "Backend Engineer");
        InterviewRound round = new InterviewRound(application, "Technical R1");
        round.setScheduledAt(Instant.parse("2026-09-05T15:00:00Z"));
        round.setInterviewerName("Jordan");
        return round;
    }

    @BeforeEach
    void setUp() {
        calendarSyncService = new CalendarSyncService(
                interviewRoundRepository, new InterviewRoundMapperImpl(), googleConnectionService, googleCalendarClient);
    }

    @Test
    void sync_createsANewEvent_whenTheRoundHasNeverSynced() {
        InterviewRound round = newRound();
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));
        when(googleConnectionService.getValidAccessToken(USER_ID)).thenReturn(Optional.of(ACCESS_TOKEN));
        when(googleCalendarClient.insertEvent(eq(ACCESS_TOKEN), any(CalendarEventData.class))).thenReturn("google-event-1");

        InterviewRoundResponse response = calendarSyncService.sync(ROUND_ID, USER_ID);

        assertThat(response.googleEventId()).isEqualTo("google-event-1");
        assertThat(round.getGoogleEventId()).isEqualTo("google-event-1");
        verify(googleCalendarClient).insertEvent(eq(ACCESS_TOKEN), any(CalendarEventData.class));
        verify(googleCalendarClient, never()).updateEvent(any(), any(), any());
    }

    @Test
    void sync_updatesTheSameEvent_ratherThanDuplicating_whenTheRoundAlreadyHasAGoogleEventId() {
        InterviewRound round = newRound();
        round.setGoogleEventId("google-event-1");
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));
        when(googleConnectionService.getValidAccessToken(USER_ID)).thenReturn(Optional.of(ACCESS_TOKEN));
        when(googleCalendarClient.updateEvent(eq(ACCESS_TOKEN), eq("google-event-1"), any(CalendarEventData.class)))
                .thenReturn("google-event-1");

        InterviewRoundResponse response = calendarSyncService.sync(ROUND_ID, USER_ID);

        assertThat(response.googleEventId()).isEqualTo("google-event-1");
        verify(googleCalendarClient).updateEvent(eq(ACCESS_TOKEN), eq("google-event-1"), any(CalendarEventData.class));
        verify(googleCalendarClient, never()).insertEvent(any(), any());
    }

    @Test
    void sync_buildsTheEventTitleAndDescriptionFromTheRoundAndItsApplication() {
        InterviewRound round = newRound();
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));
        when(googleConnectionService.getValidAccessToken(USER_ID)).thenReturn(Optional.of(ACCESS_TOKEN));
        when(googleCalendarClient.insertEvent(eq(ACCESS_TOKEN), any(CalendarEventData.class))).thenReturn("google-event-1");

        calendarSyncService.sync(ROUND_ID, USER_ID);

        var captor = org.mockito.ArgumentCaptor.forClass(CalendarEventData.class);
        verify(googleCalendarClient).insertEvent(eq(ACCESS_TOKEN), captor.capture());
        CalendarEventData event = captor.getValue();
        assertThat(event.title()).contains("Technical R1").contains("Backend Engineer").contains("Anthropic");
        assertThat(event.description()).contains("Jordan");
        assertThat(event.start()).isEqualTo(Instant.parse("2026-09-05T15:00:00Z"));
        assertThat(event.end()).isEqualTo(Instant.parse("2026-09-05T16:00:00Z"));
        assertThat(event.reminderMinutesBefore()).isEqualTo(60);
    }

    @Test
    void sync_throwsNotConnected_whenTheUserHasNoGoogleConnection() {
        InterviewRound round = newRound();
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));
        when(googleConnectionService.getValidAccessToken(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarSyncService.sync(ROUND_ID, USER_ID))
                .isInstanceOf(GoogleCalendarNotConnectedException.class);
        verify(googleCalendarClient, never()).insertEvent(any(), any());
    }

    @Test
    void sync_rejectsARoundWithNoScheduledDate() {
        InterviewRound round = newRound();
        round.setScheduledAt(null);
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));

        assertThatThrownBy(() -> calendarSyncService.sync(ROUND_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(googleConnectionService, never()).getValidAccessToken(any());
    }

    @Test
    void sync_isScopedToTheCallersOwnRounds() {
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarSyncService.sync(ROUND_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sync_neverCalledMoreThanOncePerAttempt_verifyingNoAccidentalDuplicateCreate() {
        InterviewRound round = newRound();
        when(interviewRoundRepository.findByIdAndApplicationUserId(ROUND_ID, USER_ID)).thenReturn(Optional.of(round));
        when(googleConnectionService.getValidAccessToken(USER_ID)).thenReturn(Optional.of(ACCESS_TOKEN));
        when(googleCalendarClient.insertEvent(eq(ACCESS_TOKEN), any(CalendarEventData.class))).thenReturn("google-event-1");

        calendarSyncService.sync(ROUND_ID, USER_ID);

        verify(googleCalendarClient, times(1)).insertEvent(any(), any());
    }
}
