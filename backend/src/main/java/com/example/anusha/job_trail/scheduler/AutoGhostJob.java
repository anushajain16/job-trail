package com.example.anusha.job_trail.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trigger only — all the actual query/transition logic lives in
 * {@link AutoGhostService} so it can be unit- and integration-tested by
 * calling it directly, without waiting on (or faking) the cron clock.
 */
@Component
public class AutoGhostJob {

    private static final Logger log = LoggerFactory.getLogger(AutoGhostJob.class);

    private final AutoGhostService autoGhostService;

    public AutoGhostJob(AutoGhostService autoGhostService) {
        this.autoGhostService = autoGhostService;
    }

    @Scheduled(cron = "${app.scheduling.auto-ghost-cron}")
    public void run() {
        int ghosted = autoGhostService.ghostStaleApplications();
        log.info("Auto-ghost sweep complete: {} application(s) ghosted", ghosted);
    }
}
