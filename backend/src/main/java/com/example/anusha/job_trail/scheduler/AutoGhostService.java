package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Finds applications whose deadline has elapsed and which have sat
 * untouched since, and moves each one to {@link Stage#GHOSTED} through the
 * same {@link StatusHistoryService} path a user-driven stage change uses —
 * so a ghosted-by-the-job row is indistinguishable in the history table
 * from one a user set by hand.
 */
@Service
public class AutoGhostService {

    private static final Logger log = LoggerFactory.getLogger(AutoGhostService.class);

    // SAVED is excluded because nothing was ever applied to, so there's
    // nothing to be "ghosted" by; OFFER/REJECTED/GHOSTED are already
    // terminal outcomes the job must never overwrite.
    private static final Set<Stage> INELIGIBLE_STAGES =
            EnumSet.of(Stage.SAVED, Stage.OFFER, Stage.REJECTED, Stage.GHOSTED);

    private final ApplicationRepository applicationRepository;
    private final StatusHistoryService statusHistoryService;
    private final SchedulerProperties properties;
    private final Clock clock;

    public AutoGhostService(ApplicationRepository applicationRepository, StatusHistoryService statusHistoryService,
                             SchedulerProperties properties, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.statusHistoryService = statusHistoryService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Runs one full sweep in a single transaction: candidate selection and
     * every resulting ghost-transition commit together, or not at all.
     *
     * @return how many applications were ghosted, for the job's log line.
     */
    @Transactional
    public int ghostStaleApplications() {
        LocalDate today = LocalDate.now(clock);
        Instant staleBefore = clock.instant().minus(properties.staleAfter());

        List<Application> candidates =
                applicationRepository.findGhostCandidates(INELIGIBLE_STAGES, today, staleBefore);

        for (Application application : candidates) {
            log.info("Auto-ghosting application {} ({} at {}): deadline {} elapsed, untouched since {}",
                    application.getId(), application.getRole(), application.getCompany(),
                    application.getDeadline(), application.getUpdatedAt());
            statusHistoryService.recordTransition(application, Stage.GHOSTED);
        }

        return candidates.size();
    }
}
