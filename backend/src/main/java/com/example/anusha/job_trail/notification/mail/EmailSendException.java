package com.example.anusha.job_trail.notification.mail;

/**
 * Unchecked wrapper for any failure to send an email, regardless of what
 * the underlying transport threw. Keeps {@link EmailSender} callers from
 * needing to know (or catch) a transport-specific exception type.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
