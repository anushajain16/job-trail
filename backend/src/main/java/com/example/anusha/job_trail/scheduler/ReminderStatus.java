package com.example.anusha.job_trail.scheduler;

/**
 * Outcome of one {@link ReminderLog} row's send attempt. {@code PENDING} is
 * the state a row is claimed in — before the email send is even attempted —
 * so the unique constraint backing {@link ReminderLogRepository} blocks a
 * concurrent duplicate send from the moment of the claim, not just after a
 * successful one.
 */
public enum ReminderStatus {
    PENDING,
    SENT,
    FAILED
}
