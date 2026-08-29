package com.example.anusha.job_trail.googlecalendar.exception;

/**
 * Raised when a calendar-sync (or, in principle, any other calendar
 * action) is attempted for a user with no {@code google_connection} row —
 * never connected, or already disconnected. {@code GlobalExceptionHandler}
 * maps this to a 409: the request is well-formed, but the account's
 * current state doesn't allow it yet.
 */
public class GoogleCalendarNotConnectedException extends RuntimeException {

    public GoogleCalendarNotConnectedException(String message) {
        super(message);
    }
}
