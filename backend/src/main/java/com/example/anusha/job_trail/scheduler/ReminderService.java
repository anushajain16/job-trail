package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.status.Stage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Finds applications with an upcoming deadline. Sending the actual reminder
 * is deliberately a separate bean ({@link ReminderSender}) rather than a
 * method on this one: an {@code @Async} method only runs asynchronously
 * when called *through* the Spring proxy, and a call from another method on
 * the same bean bypasses that proxy entirely — so the read side and the
 * send side have to live on different beans for {@code @Async} to actually
 * apply.
 */
@Service
public class ReminderService {

    // Same terminal/pre-application exclusions as the auto-ghost sweep: a
    // deadline reminder is pointless once an application is resolved, and
    // meaningless before it's even been applied to.
    private static final Set<Stage> INELIGIBLE_STAGES =
            EnumSet.of(Stage.SAVED, Stage.OFFER, Stage.REJECTED, Stage.GHOSTED);

    private final ApplicationRepository applicationRepository;
    private final SchedulerProperties properties;
    private final Clock clock;

    public ReminderService(ApplicationRepository applicationRepository, SchedulerProperties properties,
                            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Application> findUpcomingDeadlines() {
        LocalDate today = LocalDate.now(clock);
        // Duration isn't a day-based TemporalAmount LocalDate.plus() accepts
        // directly (it only understands seconds/nanos), so go through days.
        LocalDate lookaheadUntil = today.plusDays(properties.reminderLookahead().toDays());
        return applicationRepository.findUpcomingDeadlines(INELIGIBLE_STAGES, today, lookaheadUntil);
    }
}
