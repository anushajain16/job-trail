package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Builds the subject and HTML body of one deadline-reminder email. Kept
 * separate from {@link ReminderSender} so the "what does a reminder say"
 * question lives in its own place, distinct from "how does an email
 * actually go out" ({@code EmailSender}) and "did we already send this one"
 * ({@link ReminderLog}).
 */
@Component
class ReminderEmailContent {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

    String subject(Application application, long daysUntilDeadline) {
        String when = daysUntilDeadline <= 0 ? "today" : "in " + daysUntilDeadline + " day(s)";
        return "Deadline reminder: %s at %s is due %s".formatted(application.getRole(), application.getCompany(),
                when);
    }

    String html(Application application, long daysUntilDeadline) {
        String when = daysUntilDeadline <= 0
                ? "<strong>today</strong>"
                : "in <strong>" + daysUntilDeadline + " day(s)</strong>";
        return """
                <!DOCTYPE html>
                <html>
                  <body style="font-family: sans-serif; color: #1a1a1a; line-height: 1.5;">
                    <p>Hi,</p>
                    <p>
                      Your application for <strong>%s</strong> at <strong>%s</strong>
                      has a deadline %s &mdash; on %s.
                    </p>
                    <p>Open JobTrail to review or update this application.</p>
                    <p style="color: #666; font-size: 0.85em;">
                      You're getting this because the application has a deadline coming up
                      and hasn't reached a final stage yet.
                    </p>
                  </body>
                </html>
                """.formatted(escape(application.getRole()), escape(application.getCompany()), when,
                application.getDeadline().format(DEADLINE_FORMAT));
    }

    // Only company/role are ever interpolated as untrusted free text into
    // this HTML — every other value here is either our own literal markup
    // or a formatted date, so this is the one escape this template needs.
    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
