package com.example.anusha.job_trail.googlecalendar;

/**
 * Request body shape for {@code POST}/{@code PUT} against the Calendar v3
 * {@code events} resource — only the fields {@link GoogleCalendarApiClient}
 * actually sets.
 */
record GoogleEventRequest(
        String summary,
        String description,
        GoogleEventTime start,
        GoogleEventTime end,
        GoogleEventReminders reminders
) {
}
