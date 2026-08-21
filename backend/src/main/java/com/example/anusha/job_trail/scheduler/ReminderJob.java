package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trigger only. Finding candidates ({@link ReminderService}, transactional,
 * synchronous) and sending each reminder ({@link ReminderSender},
 * {@code @Async}) are split across two beans on purpose — see
 * {@link ReminderService}'s Javadoc for why.
 */
@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final ReminderService reminderService;
    private final ReminderSender reminderSender;

    public ReminderJob(ReminderService reminderService, ReminderSender reminderSender) {
        this.reminderService = reminderService;
        this.reminderSender = reminderSender;
    }

    @Scheduled(cron = "${app.scheduling.reminder-cron}")
    public void run() {
        List<Application> upcoming = reminderService.findUpcomingDeadlines();
        upcoming.forEach(reminderSender::sendReminder);
        log.info("Reminder sweep complete: {} reminder(s) dispatched", upcoming.size());
    }
}
