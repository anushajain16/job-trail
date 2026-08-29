package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.common.exception.ResourceNotFoundException;
import com.example.anusha.job_trail.googlecalendar.dto.CalendarEventData;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarNotConnectedException;
import com.example.anusha.job_trail.interview.InterviewRound;
import com.example.anusha.job_trail.interview.InterviewRoundMapper;
import com.example.anusha.job_trail.interview.InterviewRoundRepository;
import com.example.anusha.job_trail.interview.dto.InterviewRoundResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The "CalendarService" of this feature: given one interview round the
 * caller owns, creates or updates the Google Calendar event for it and
 * records the resulting event id on the round. Explicit-only — there is no
 * automatic-on-create trigger, by design (see the package doc): the caller
 * always reaches this through a POST the user asked for, e.g. an
 * "Add to Calendar" button.
 */
@Service
public class CalendarSyncService {

    // No duration field on InterviewRound to derive this from — an hour is
    // a reasonable default for a single interview round, and matches what
    // the frontend form's "Date" field alone implies (a point in time).
    private static final Duration DEFAULT_EVENT_DURATION = Duration.ofHours(1);
    private static final int REMINDER_MINUTES_BEFORE = 60;

    private final InterviewRoundRepository interviewRoundRepository;
    private final InterviewRoundMapper interviewRoundMapper;
    private final GoogleConnectionService googleConnectionService;
    private final GoogleCalendarClient googleCalendarClient;

    public CalendarSyncService(InterviewRoundRepository interviewRoundRepository, InterviewRoundMapper interviewRoundMapper,
                                GoogleConnectionService googleConnectionService, GoogleCalendarClient googleCalendarClient) {
        this.interviewRoundRepository = interviewRoundRepository;
        this.interviewRoundMapper = interviewRoundMapper;
        this.googleConnectionService = googleConnectionService;
        this.googleCalendarClient = googleCalendarClient;
    }

    @Transactional
    public InterviewRoundResponse sync(UUID interviewRoundId, UUID userId) {
        InterviewRound round = interviewRoundRepository.findByIdAndApplicationUserId(interviewRoundId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + interviewRoundId));

        if (round.getScheduledAt() == null) {
            throw new IllegalArgumentException("Interview round has no scheduled date to sync to a calendar");
        }

        String accessToken = googleConnectionService.getValidAccessToken(userId)
                .orElseThrow(() -> new GoogleCalendarNotConnectedException(
                        "No Google Calendar connected — connect one before syncing an interview round"));

        CalendarEventData eventData = buildEventData(round);

        // The one line that makes this create-or-update rather than
        // always-create: a round with no google_event_id yet has never
        // synced, so it's a new event; one that already has an id gets
        // that same event overwritten in place.
        String eventId = round.getGoogleEventId() == null
                ? googleCalendarClient.insertEvent(accessToken, eventData)
                : googleCalendarClient.updateEvent(accessToken, round.getGoogleEventId(), eventData);

        round.setGoogleEventId(eventId);
        return interviewRoundMapper.toResponse(round);
    }

    private static CalendarEventData buildEventData(InterviewRound round) {
        Application application = round.getApplication();
        String title = "%s: %s at %s".formatted(round.getRoundType(), application.getRole(), application.getCompany());

        StringBuilder description = new StringBuilder();
        description.append("Round: ").append(round.getRoundType()).append('\n');
        description.append("Company: ").append(application.getCompany()).append('\n');
        description.append("Role: ").append(application.getRole());
        if (round.getInterviewerName() != null && !round.getInterviewerName().isBlank()) {
            description.append("\nInterviewer: ").append(round.getInterviewerName());
        }
        if (round.getQuestionsAsked() != null && !round.getQuestionsAsked().isBlank()) {
            description.append("\n\nQuestions asked:\n").append(round.getQuestionsAsked());
        }

        Instant start = round.getScheduledAt();
        return new CalendarEventData(title, description.toString(), start, start.plus(DEFAULT_EVENT_DURATION), REMINDER_MINUTES_BEFORE);
    }
}
