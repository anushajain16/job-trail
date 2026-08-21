package com.example.anusha.job_trail.scheduler;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistory;
import com.example.anusha.job_trail.status.StatusHistoryRepository;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Against the real app context and the real (dockerized) Postgres behind
 * the "test" profile — no mocks. The "test" profile pins both jobs' cron
 * expressions to {@code "-"} (Spring's documented "never fire"), so nothing
 * here races a real sweep; every job service is invoked directly to control
 * timing exactly. Seeding bypasses {@link com.example.anusha.job_trail.status.StatusHistoryService}
 * and backdates {@code updated_at} with a raw SQL update — {@code @LastModifiedDate}
 * auditing has no setter and always stamps "now" on save, the same trick
 * {@code AnalyticsServiceIntegrationTest} uses for {@code changed_at}.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchedulerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private AutoGhostService autoGhostService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private AutoGhostJob autoGhostJob;

    @Autowired
    private ReminderJob reminderJob;

    @Autowired
    private SchedulerProperties schedulerProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private User newUser() {
        return userRepository.saveAndFlush(new User("user-" + UUID.randomUUID() + "@jobtrail.dev", "hash"));
    }

    private Application seedApplication(User user, Stage stage, LocalDate deadline, Instant lastActivity) {
        Application application = new Application(user, "Company", "Role");
        application.setCurrentStage(stage);
        application.setDeadline(deadline);
        application = applicationRepository.saveAndFlush(application);
        statusHistoryRepository.saveAndFlush(new StatusHistory(application, stage));
        jdbcTemplate.update("UPDATE applications SET updated_at = ? WHERE id = ?",
                Timestamp.from(lastActivity), application.getId());
        return application;
    }

    @Test
    void ghostsAStaleApplicationPastItsDeadline_andWritesHistory() {
        User user = newUser();
        Instant longAgo = clock.instant().minus(schedulerProperties.staleAfter()).minus(1, ChronoUnit.DAYS);
        Application application = seedApplication(user, Stage.APPLIED,
                LocalDate.now(clock).minusDays(20), longAgo);

        int ghosted = autoGhostService.ghostStaleApplications();
        assertThat(ghosted).isGreaterThanOrEqualTo(1);

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStage()).isEqualTo(Stage.GHOSTED);

        List<StatusHistory> history =
                statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId());
        assertThat(history).extracting(StatusHistory::getStage).containsExactly(Stage.APPLIED, Stage.GHOSTED);
    }

    @Test
    void leavesARecentlyUpdatedApplicationAlone_evenPastItsDeadline() {
        User user = newUser();
        Application application = seedApplication(user, Stage.APPLIED,
                LocalDate.now(clock).minusDays(20), clock.instant().minus(1, ChronoUnit.HOURS));

        autoGhostService.ghostStaleApplications();

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStage()).isEqualTo(Stage.APPLIED);
    }

    @Test
    void leavesAStaleApplicationWithNoDeadlineAlone() {
        User user = newUser();
        Instant longAgo = clock.instant().minus(schedulerProperties.staleAfter()).minus(1, ChronoUnit.DAYS);
        Application application = seedApplication(user, Stage.APPLIED, null, longAgo);

        autoGhostService.ghostStaleApplications();

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStage()).isEqualTo(Stage.APPLIED);
    }

    @Test
    void neverReGhostsAnApplicationAlreadyInATerminalStage() {
        User user = newUser();
        Instant longAgo = clock.instant().minus(schedulerProperties.staleAfter()).minus(1, ChronoUnit.DAYS);
        Application application = seedApplication(user, Stage.REJECTED,
                LocalDate.now(clock).minusDays(20), longAgo);

        autoGhostService.ghostStaleApplications();

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getCurrentStage()).isEqualTo(Stage.REJECTED);
    }

    @Test
    void reminderService_findsApplicationsWithinTheLookaheadWindowOnly() {
        User user = newUser();
        Application dueSoon = seedApplication(user, Stage.APPLIED,
                LocalDate.now(clock).plusDays(1), clock.instant());
        Application dueLater = seedApplication(user, Stage.APPLIED,
                LocalDate.now(clock).plusDays(schedulerProperties.reminderLookahead().toDays() + 30),
                clock.instant());

        List<Application> upcoming = reminderService.findUpcomingDeadlines();

        assertThat(upcoming).extracting(Application::getId).contains(dueSoon.getId());
        assertThat(upcoming).extracting(Application::getId).doesNotContain(dueLater.getId());
    }

    @Test
    void bothJobsAreWiredUpAsScheduledTasksBoundToTheirConfiguredCronProperties() throws NoSuchMethodException {
        assertThat(autoGhostJob).isNotNull();
        assertThat(reminderJob).isNotNull();

        Method ghostRun = AutoGhostJob.class.getDeclaredMethod("run");
        assertThat(ghostRun.getAnnotation(Scheduled.class).cron()).isEqualTo("${app.scheduling.auto-ghost-cron}");

        Method reminderRun = ReminderJob.class.getDeclaredMethod("run");
        assertThat(reminderRun.getAnnotation(Scheduled.class).cron()).isEqualTo("${app.scheduling.reminder-cron}");
    }
}
