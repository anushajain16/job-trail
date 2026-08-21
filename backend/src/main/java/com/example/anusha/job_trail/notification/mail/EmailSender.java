package com.example.anusha.job_trail.notification.mail;

/**
 * The only contract a caller needs to send an HTML email. Nothing here
 * mentions SMTP, a vendor SDK, or any other transport detail — that's the
 * whole point: a caller (e.g. the reminder scheduler) depends on this
 * interface, never on {@link SmtpEmailSender} or any other implementation
 * directly, so the transport can be swapped without touching callers.
 */
public interface EmailSender {

    /**
     * Sends one HTML email. Implementations should throw
     * {@link EmailSendException} (never a transport-specific checked
     * exception) on failure, so callers can catch one thing regardless of
     * how the email actually goes out.
     *
     * @param to      recipient address
     * @param subject email subject line
     * @param htmlBody the email body, as HTML
     */
    void send(String to, String subject, String htmlBody);
}
