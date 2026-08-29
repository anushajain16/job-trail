package com.example.anusha.job_trail.googlecalendar;

import com.example.anusha.job_trail.googlecalendar.dto.CalendarEventData;

/**
 * The one seam between {@link CalendarSyncService} and the real Google
 * Calendar API — deliberately narrow (create, update, nothing else) and an
 * interface specifically so unit tests can mock it instead of ever making
 * a real call to Google. {@link GoogleCalendarApiClient} is the only
 * production implementation.
 */
public interface GoogleCalendarClient {

    /** Creates a new event on the caller's primary calendar and returns
     * its Google-assigned event id. */
    String insertEvent(String accessToken, CalendarEventData event);

    /** Overwrites the event {@code googleEventId} in place — the same
     * event id comes back, since the event isn't being recreated. */
    String updateEvent(String accessToken, String googleEventId, CalendarEventData event);
}
