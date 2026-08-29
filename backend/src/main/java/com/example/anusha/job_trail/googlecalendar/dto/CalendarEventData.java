package com.example.anusha.job_trail.googlecalendar.dto;

import java.time.Instant;

/**
 * Everything {@link com.example.anusha.job_trail.googlecalendar.GoogleCalendarClient}
 * needs to create or update one Google Calendar event — deliberately not
 * {@code InterviewRound} itself, so the client stays ignorant of this
 * app's domain model and only {@code CalendarSyncService} knows how one is
 * built from a round.
 */
public record CalendarEventData(
        String title,
        String description,
        Instant start,
        Instant end,
        // Minutes before start a popup reminder should fire; null means
        // "no override" (the calendar's own default reminders apply).
        Integer reminderMinutesBefore
) {
}
