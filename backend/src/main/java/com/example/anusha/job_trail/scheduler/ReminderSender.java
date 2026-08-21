package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The actual "send" for one deadline reminder. There's no email/push
 * channel wired up yet, so this is a log line for now — but it's the one
 * place a real channel would plug in later, and it's already off the
 * scheduler thread via {@code @Async}, so adding a slow network call here
 * later won't block the sweep that finds candidates.
 */
@Component
public class ReminderSender {

    private static final Logger log = LoggerFactory.getLogger(ReminderSender.class);

    private final Clock clock;

    public ReminderSender(Clock clock) {
        this.clock = clock;
    }

    // Runs on the "taskExecutor" pool from AsyncConfig (see AsyncConfig),
    // not on whatever thread called this.
    @Async("taskExecutor")
    public void sendReminder(Application application) {
        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(clock), application.getDeadline());
        log.info("Reminder: application {} ({} at {}) has a deadline in {} day(s), on {}",
                application.getId(), application.getRole(), application.getCompany(),
                daysUntilDeadline, application.getDeadline());
    }
}
