package com.example.anusha.job_trail.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code app.scheduling.*}. Both jobs get their own cron expression so
 * either can be retimed (or, via an unreachable cron like {@code "-"}, never
 * fired) independently — see {@link AutoGhostJob} and {@link ReminderJob}.
 *
 * @param autoGhostCron   when the auto-ghost sweep runs, as a standard
 *                        6-field Spring cron expression.
 * @param staleAfter      how long an eligible application must sit untouched
 *                        (no stage change, no edit) past its deadline before
 *                        the sweep will ghost it. Guards against ghosting an
 *                        application the user is actively working, just
 *                        because its deadline happened to pass.
 * @param reminderCron    when the upcoming-deadline reminder sweep runs.
 * @param reminderLookahead how far ahead of today a deadline still counts as
 *                        "upcoming" for the reminder sweep.
 */
@ConfigurationProperties(prefix = "app.scheduling")
public record SchedulerProperties(
        String autoGhostCron,
        Duration staleAfter,
        String reminderCron,
        Duration reminderLookahead
) {
}
