package com.example.anusha.job_trail.notification.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * The one {@link EmailSender} implementation the app ships today: plain
 * SMTP via Spring's {@link JavaMailSender}, configured entirely through
 * {@code spring.mail.*} (host/port/credentials — see application.yml). SMTP
 * is itself provider-agnostic — a local dev catch-all, SES, SendGrid's SMTP
 * relay, etc. are all just different config for the same transport — so
 * this class never needs to know which one is on the other end.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    // Transient SMTP hiccups (a relay momentarily unreachable, a connection
    // reset) are common enough, and cheap enough to retry, that it's worth
    // absorbing a couple here rather than letting them fail a whole day's
    // reminder straight to "try again tomorrow".
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 500;

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public SmtpEmailSender(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        MailException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailSender.send(buildMessage(to, subject, htmlBody));
                return;
            } catch (MailException e) {
                lastFailure = e;
                log.warn("Attempt {}/{} to email {} failed", attempt, MAX_ATTEMPTS, to, e);
                if (attempt < MAX_ATTEMPTS) {
                    sleep(RETRY_BACKOFF_MILLIS * attempt);
                }
            }
        }
        throw new EmailSendException("Failed to send email to " + to + " after " + MAX_ATTEMPTS + " attempt(s)",
                lastFailure);
    }

    private MimeMessage buildMessage(String to, String subject, String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
        } catch (Exception e) {
            // MessagingException from the helper itself (bad address, etc.) —
            // not a send-time failure, so it doesn't get the retry loop above.
            throw new EmailSendException("Failed to build email to " + to, e);
        }
        return message;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
