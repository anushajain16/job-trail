/**
 * Background jobs: no APIs of its own, everything here runs off a cron
 * schedule. Auto-ghost ({@link com.example.anusha.job_trail.scheduler.AutoGhostJob})
 * moves stale, past-deadline applications to {@code GHOSTED} through the
 * same append-only history path a user-driven stage change uses. Reminder
 * ({@link com.example.anusha.job_trail.scheduler.ReminderJob}) logs (later:
 * sends) a heads-up for applications with an upcoming deadline.
 */
package com.example.anusha.job_trail.scheduler;
