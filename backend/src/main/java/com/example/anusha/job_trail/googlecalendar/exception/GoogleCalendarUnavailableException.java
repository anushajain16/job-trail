package com.example.anusha.job_trail.googlecalendar.exception;

/**
 * Raised when a real call to Google (token exchange, token refresh, or the
 * Calendar API itself) fails — network failure, a non-2xx response, an
 * unusable body. Same "nothing to gracefully fall back to, so surface it"
 * reasoning as {@code matching.exception.MlServiceUnavailableException};
 * {@code GlobalExceptionHandler} maps it to a 502.
 */
public class GoogleCalendarUnavailableException extends RuntimeException {

    public GoogleCalendarUnavailableException(String message) {
        super(message);
    }

    public GoogleCalendarUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
