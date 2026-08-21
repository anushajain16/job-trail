/**
 * Background jobs: no APIs of its own, everything here runs off a cron
 * schedule. Auto-ghost ({@link com.example.anusha.job_trail.scheduler.AutoGhostJob})
 * moves stale, past-deadline applications to {@code GHOSTED} through the
 * same append-only history path a user-driven stage change uses. Reminder
 * ({@link com.example.anusha.job_trail.scheduler.ReminderJob}) emails a
 * heads-up for applications with an upcoming deadline — the actual mail
 * transport lives in {@link com.example.anusha.job_trail.notification.mail},
 * kept separate so this package never depends on a specific provider, and
 * duplicate-send protection lives in
 * {@link com.example.anusha.job_trail.scheduler.ReminderLog}.
 */
package com.example.anusha.job_trail.scheduler;
