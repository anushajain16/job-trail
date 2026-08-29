package com.example.anusha.job_trail.analytics;

import com.example.anusha.job_trail.common.AbstractIntegrationTest;
import com.example.anusha.job_trail.analytics.dto.ConversionResponse;
import com.example.anusha.job_trail.analytics.dto.FunnelResponse;
import com.example.anusha.job_trail.analytics.dto.ResumePerformanceResponse;
import com.example.anusha.job_trail.analytics.dto.ResumeVersionPerformance;
import com.example.anusha.job_trail.analytics.dto.SourceResponseRate;
import com.example.anusha.job_trail.analytics.dto.StageConversion;
import com.example.anusha.job_trail.analytics.dto.StageDuration;
import com.example.anusha.job_trail.analytics.dto.TimeInStageResponse;
import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.application.ApplicationRepository;
import com.example.anusha.job_trail.document.Document;
import com.example.anusha.job_trail.document.DocumentRepository;
import com.example.anusha.job_trail.document.DocumentType;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistory;
import com.example.anusha.job_trail.status.StatusHistoryRepository;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Seeds a hand-computed status_history against a real Postgres started by
 * Testcontainers (see AbstractIntegrationTest), then checks AnalyticsService's funnel,
 * conversion, and time-in-stage math against numbers worked out by hand.
 *
 * <p>Rows are written straight through the repositories rather than the
 * stage-change API, and their {@code changed_at} is backdated with a raw
 * SQL update afterward — {@code @CreatedDate} auditing has no setter and
 * always stamps "now" on insert, and a test that only has sub-second gaps
 * between transitions can't assert a days-level average against anything
 * meaningful.
 */
@SpringBootTest
class AnalyticsServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private User newUser() {
        return userRepository.saveAndFlush(new User("user-" + UUID.randomUUID() + "@jobtrail.dev", "hash"));
    }

    private Application newApplication(User user, String source) {
        Application application = new Application(user, "Company", "Role");
        application.setSource(source);
        return applicationRepository.saveAndFlush(application);
    }

    private Document newResumeVersion(User user, String label) {
        return documentRepository.saveAndFlush(new Document(user, DocumentType.RESUME, label,
                "key-" + UUID.randomUUID(), "resume.pdf", "application/pdf", 1024L));
    }

    // Writes one history row and backdates its changed_at to T0 + dayOffset.
    private void recordAt(Application application, Stage stage, int dayOffset) {
        StatusHistory row = statusHistoryRepository.saveAndFlush(new StatusHistory(application, stage));
        jdbcTemplate.update("UPDATE status_history SET changed_at = ? WHERE id = ?",
                Timestamp.from(T0.plus(dayOffset, ChronoUnit.DAYS)), row.getId());
    }

    /**
     * Three applications for one user:
     * <ul>
     *   <li>App1 (LinkedIn): SAVED(0) -> APPLIED(1) -> SCREEN(3) -> REJECTED(7) — responded, dropped after SCREEN.</li>
     *   <li>App2 (LinkedIn): SAVED(0) -> APPLIED(2) -> GHOSTED(9) — never responded.</li>
     *   <li>App3 (Referral): SAVED(0) -> APPLIED(1) -> SCREEN(3) -> INTERVIEW(6) -> FINAL(8) -> OFFER(9) — full funnel.</li>
     * </ul>
     */
    @Test
    void funnelConversionAndTimeInStage_matchHandComputedValues() {
        User user = newUser();

        Application app1 = newApplication(user, "LinkedIn");
        recordAt(app1, Stage.SAVED, 0);
        recordAt(app1, Stage.APPLIED, 1);
        recordAt(app1, Stage.SCREEN, 3);
        recordAt(app1, Stage.REJECTED, 7);

        Application app2 = newApplication(user, "LinkedIn");
        recordAt(app2, Stage.SAVED, 0);
        recordAt(app2, Stage.APPLIED, 2);
        recordAt(app2, Stage.GHOSTED, 9);

        Application app3 = newApplication(user, "Referral");
        recordAt(app3, Stage.SAVED, 0);
        recordAt(app3, Stage.APPLIED, 1);
        recordAt(app3, Stage.SCREEN, 3);
        recordAt(app3, Stage.INTERVIEW, 6);
        recordAt(app3, Stage.FINAL, 8);
        recordAt(app3, Stage.OFFER, 9);

        // --- funnel: SAVED 3, APPLIED 3, SCREEN 2, INTERVIEW 1, FINAL 1, OFFER 1 ---
        FunnelResponse funnel = analyticsService.funnel(user.getId());
        assertThat(funnel.totalApplications()).isEqualTo(3);
        Map<Stage, Long> reached = funnel.stages().stream()
                .collect(Collectors.toMap(s -> s.stage(), s -> s.applications()));
        assertThat(reached)
                .containsEntry(Stage.SAVED, 3L)
                .containsEntry(Stage.APPLIED, 3L)
                .containsEntry(Stage.SCREEN, 2L)
                .containsEntry(Stage.INTERVIEW, 1L)
                .containsEntry(Stage.FINAL, 1L)
                .containsEntry(Stage.OFFER, 1L);

        // --- conversion: SAVED->APPLIED 1.0, APPLIED->SCREEN 2/3, SCREEN->INTERVIEW 0.5, INTERVIEW->FINAL 1.0, FINAL->OFFER 1.0 ---
        ConversionResponse conversion = analyticsService.conversion(user.getId());
        List<StageConversion> stageConversions = conversion.stageConversions();
        assertThat(stageConversions).hasSize(5);
        assertThat(rateFor(stageConversions, Stage.SAVED, Stage.APPLIED)).isCloseTo(1.0, within(1e-9));
        assertThat(rateFor(stageConversions, Stage.APPLIED, Stage.SCREEN)).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(rateFor(stageConversions, Stage.SCREEN, Stage.INTERVIEW)).isCloseTo(0.5, within(1e-9));
        assertThat(rateFor(stageConversions, Stage.INTERVIEW, Stage.FINAL)).isCloseTo(1.0, within(1e-9));
        assertThat(rateFor(stageConversions, Stage.FINAL, Stage.OFFER)).isCloseTo(1.0, within(1e-9));

        // --- response rate by source: LinkedIn 1/2 (App1 responded, App2 ghosted), Referral 1/1 ---
        List<SourceResponseRate> bySource = conversion.responseRateBySource();
        SourceResponseRate linkedIn = bySource.stream().filter(r -> r.source().equals("LinkedIn")).findFirst().orElseThrow();
        assertThat(linkedIn.totalApplications()).isEqualTo(2);
        assertThat(linkedIn.respondedApplications()).isEqualTo(1);
        assertThat(linkedIn.responseRate()).isCloseTo(0.5, within(1e-9));

        SourceResponseRate referral = bySource.stream().filter(r -> r.source().equals("Referral")).findFirst().orElseThrow();
        assertThat(referral.totalApplications()).isEqualTo(1);
        assertThat(referral.respondedApplications()).isEqualTo(1);
        assertThat(referral.responseRate()).isCloseTo(1.0, within(1e-9));

        // --- time in stage (days), averaged over completed transitions ---
        // SAVED:   [1, 2, 1] -> 4/3
        // APPLIED: [2, 7, 2] -> 11/3
        // SCREEN:  [4, 3]    -> 3.5   (App1 SCREEN->REJECTED, App3 SCREEN->INTERVIEW)
        // INTERVIEW: [2]     -> 2.0
        // FINAL:   [1]       -> 1.0
        TimeInStageResponse timeInStage = analyticsService.timeInStage(user.getId());
        assertDuration(timeInStage.stages(), Stage.SAVED, 4.0 / 3, 3);
        assertDuration(timeInStage.stages(), Stage.APPLIED, 11.0 / 3, 3);
        assertDuration(timeInStage.stages(), Stage.SCREEN, 3.5, 2);
        assertDuration(timeInStage.stages(), Stage.INTERVIEW, 2.0, 1);
        assertDuration(timeInStage.stages(), Stage.FINAL, 1.0, 1);
    }

    /**
     * Three resume versions (A, B, and an unused C) attached across four
     * applications, one of which sends no resume version at all:
     * <ul>
     *   <li>App1 (resume A): SAVED -> APPLIED -> SCREEN -> REJECTED — responded.</li>
     *   <li>App2 (resume A): SAVED -> APPLIED -> GHOSTED — never responded.</li>
     *   <li>App3 (resume B): full funnel to OFFER — responded.</li>
     *   <li>App4 (no resume version): SAVED -> APPLIED — excluded entirely,
     *       it can't be attributed to any version.</li>
     * </ul>
     * Expected: A = 1/2 (0.5), B = 1/1 (1.0), C = 0/0 (0.0, still listed).
     */
    @Test
    void resumePerformance_matchesHandComputedRatesAndExcludesUnlinkedApplications() {
        User user = newUser();
        Document resumeA = newResumeVersion(user, "Resume A");
        Document resumeB = newResumeVersion(user, "Resume B");
        Document resumeC = newResumeVersion(user, "Resume C");

        Application app1 = newApplication(user, "LinkedIn");
        app1.setResumeVersion(resumeA);
        applicationRepository.saveAndFlush(app1);
        recordAt(app1, Stage.SAVED, 0);
        recordAt(app1, Stage.APPLIED, 1);
        recordAt(app1, Stage.SCREEN, 3);
        recordAt(app1, Stage.REJECTED, 7);

        Application app2 = newApplication(user, "LinkedIn");
        app2.setResumeVersion(resumeA);
        applicationRepository.saveAndFlush(app2);
        recordAt(app2, Stage.SAVED, 0);
        recordAt(app2, Stage.APPLIED, 2);
        recordAt(app2, Stage.GHOSTED, 9);

        Application app3 = newApplication(user, "Referral");
        app3.setResumeVersion(resumeB);
        applicationRepository.saveAndFlush(app3);
        recordAt(app3, Stage.SAVED, 0);
        recordAt(app3, Stage.APPLIED, 1);
        recordAt(app3, Stage.SCREEN, 3);
        recordAt(app3, Stage.INTERVIEW, 6);
        recordAt(app3, Stage.FINAL, 8);
        recordAt(app3, Stage.OFFER, 9);

        Application app4 = newApplication(user, "Referral");
        recordAt(app4, Stage.SAVED, 0);
        recordAt(app4, Stage.APPLIED, 1);

        ResumePerformanceResponse response = analyticsService.resumePerformance(user.getId());
        Map<String, ResumeVersionPerformance> byLabel = response.versions().stream()
                .collect(Collectors.toMap(ResumeVersionPerformance::label, v -> v));

        assertThat(byLabel).containsKeys("Resume A", "Resume B", "Resume C");

        ResumeVersionPerformance a = byLabel.get("Resume A");
        assertThat(a.totalApplications()).isEqualTo(2);
        assertThat(a.respondedApplications()).isEqualTo(1);
        assertThat(a.responseRate()).isCloseTo(0.5, within(1e-9));

        ResumeVersionPerformance b = byLabel.get("Resume B");
        assertThat(b.totalApplications()).isEqualTo(1);
        assertThat(b.respondedApplications()).isEqualTo(1);
        assertThat(b.responseRate()).isCloseTo(1.0, within(1e-9));

        ResumeVersionPerformance c = byLabel.get("Resume C");
        assertThat(c.totalApplications()).isEqualTo(0);
        assertThat(c.respondedApplications()).isEqualTo(0);
        assertThat(c.responseRate()).isCloseTo(0.0, within(1e-9));
    }

    private static double rateFor(List<StageConversion> conversions, Stage from, Stage to) {
        return conversions.stream()
                .filter(c -> c.fromStage() == from && c.toStage() == to)
                .findFirst().orElseThrow()
                .conversionRate();
    }

    private static void assertDuration(List<StageDuration> stages, Stage stage, double expectedDays, long expectedSampleSize) {
        StageDuration duration = stages.stream().filter(d -> d.stage() == stage).findFirst().orElseThrow();
        assertThat(duration.averageDays()).isCloseTo(expectedDays, within(1e-9));
        assertThat(duration.sampleSize()).isEqualTo(expectedSampleSize);
    }
}
