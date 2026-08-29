package com.example.anusha.job_trail.googlecalendar;

/** Only the field {@link GoogleCalendarApiClient} reads back — Calendar
 * v3's actual event resource carries dozens more. */
record GoogleEventResponse(String id) {
}
