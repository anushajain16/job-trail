package com.example.anusha.job_trail.googlecalendar;

/** Calendar v3's {@code EventDateTime} — always a UTC {@code dateTime}
 * ("...Z" instant), never the date-only, all-day-event alternative field. */
record GoogleEventTime(String dateTime) {
}
