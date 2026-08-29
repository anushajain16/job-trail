package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.googlecalendar.dto.CalendarEventData;
import com.example.anusha.job_trail.googlecalendar.exception.GoogleCalendarUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Talks to the Calendar v3 REST API directly (no generated SDK client) —
 * same "plain RestClient" shape the rest of this app uses for every other
 * outbound call. The caller's own primary calendar is always the target;
 * there's no UI here for picking a different one.
 */
@Component
public class GoogleCalendarApiClient implements GoogleCalendarClient {

    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private final RestClient restClient = RestClient.create();

    @Override
    public String insertEvent(String accessToken, CalendarEventData event) {
        try {
            GoogleEventResponse response = restClient.post()
                    .uri(EVENTS_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(toRequest(event))
                    .retrieve()
                    .body(GoogleEventResponse.class);
            return requireId(response);
        } catch (RestClientException e) {
            throw new GoogleCalendarUnavailableException("Failed to create the calendar event", e);
        }
    }

    @Override
    public String updateEvent(String accessToken, String googleEventId, CalendarEventData event) {
        try {
            GoogleEventResponse response = restClient.put()
                    .uri(EVENTS_URL + "/{eventId}", googleEventId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(toRequest(event))
                    .retrieve()
                    .body(GoogleEventResponse.class);
            return requireId(response);
        } catch (RestClientException e) {
            throw new GoogleCalendarUnavailableException("Failed to update the calendar event", e);
        }
    }

    private static GoogleEventRequest toRequest(CalendarEventData event) {
        GoogleEventReminders reminders = event.reminderMinutesBefore() == null
                ? null
                : new GoogleEventReminders(false, List.of(new GoogleEventReminderOverride("popup", event.reminderMinutesBefore())));
        return new GoogleEventRequest(
                event.title(),
                event.description(),
                new GoogleEventTime(DateTimeFormatter.ISO_INSTANT.format(event.start())),
                new GoogleEventTime(DateTimeFormatter.ISO_INSTANT.format(event.end())),
                reminders
        );
    }

    private static String requireId(GoogleEventResponse response) {
        if (response == null || response.id() == null) {
            throw new GoogleCalendarUnavailableException("Google Calendar returned an empty response");
        }
        return response.id();
    }
}
