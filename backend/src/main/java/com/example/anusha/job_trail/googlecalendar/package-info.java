/**
 * Google Calendar integration: an OAuth2 connect flow (authorization code,
 * offline access, {@code calendar.events} scope) separate from the app's
 * login-only Google OAuth, plus syncing an
 * {@link com.example.anusha.job_trail.interview.InterviewRound} to a
 * calendar event on demand (the "Add to Calendar" button — never
 * automatic, so connecting a calendar is never a surprise source of
 * events).
 */
package com.example.anusha.job_trail.googlecalendar;
