package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.notification.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The actual "send" for one deadline reminder: an HTML email, via whatever
 * {@link EmailSender} is wired up — this class knows nothing about SMTP or
 * any other transport, only that it can hand an address, a subject and an
 * HTML body to something that delivers them. Already off the scheduler
 * thread via {@code @Async}, so a slow network call here never blocks the
 * sweep that finds candidates.
 *
 * <p>Duplicate-send protection lives here too, via {@link ReminderLog}:
 * before ever calling the mailer, this claims a {@code PENDING} row for
 * (application, today). The claim is a plain insert guarded by a DB unique
 * constraint (see the V7 migration) — so it's safe even against a retried
 * job run, an app restart mid-sweep, or more than one instance scheduled:
 * whichever attempt's insert loses the race gets a constraint violation and
 * simply skips sending, rather than every attempt independently deciding
 * "not sent yet" off a read that's already stale.
 */
@Component
public class ReminderSender {

    private static final Logger log = LoggerFactory.getLogger(ReminderSender.class);

    private final EmailSender emailSender;
    private final ReminderEmailContent emailContent;
    private final ReminderLogRepository reminderLogRepository;
    private final Clock clock;

    public ReminderSender(EmailSender emailSender, ReminderEmailContent emailContent,
                           ReminderLogRepository reminderLogRepository, Clock clock) {
        this.emailSender = emailSender;
        this.emailContent = emailContent;
        this.reminderLogRepository = reminderLogRepository;
        this.clock = clock;
    }

    // Runs on the "taskExecutor" pool from AsyncConfig (see AsyncConfig),
    // not on whatever thread called this.
    @Async("taskExecutor")
    public void sendReminder(Application application) {
        LocalDate today = LocalDate.now(clock);

        ReminderLog claim;
        try {
            // Each repository call is its own transaction, so the claim
            // commits (or fails) before the email send is even attempted —
            // no DB transaction is held open across the network call below.
            claim = reminderLogRepository.save(new ReminderLog(application, today));
        } catch (DataIntegrityViolationException e) {
            log.debug("Reminder for application {} on {} already claimed, skipping", application.getId(), today);
            return;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(today, application.getDeadline());
        try {
            emailSender.send(application.getUser().getEmail(),
                    emailContent.subject(application, daysUntilDeadline),
                    emailContent.html(application, daysUntilDeadline));
            claim.setStatus(ReminderStatus.SENT);
            claim.setSentAt(clock.instant());
            log.info("Reminder sent: application {} ({} at {}) has a deadline in {} day(s), on {}",
                    application.getId(), application.getRole(), application.getCompany(),
                    daysUntilDeadline, application.getDeadline());
        } catch (Exception e) {
            // Left as FAILED rather than deleted: the claim for *today*
            // stays taken (no infinite retry loop within the same sweep),
            // and if the application is still within the lookahead window
            // tomorrow, tomorrow's run claims a fresh row for that new date
            // and tries again.
            claim.setStatus(ReminderStatus.FAILED);
            log.error("Failed to send reminder email for application {}", application.getId(), e);
        }
        reminderLogRepository.save(claim);
    }
}
