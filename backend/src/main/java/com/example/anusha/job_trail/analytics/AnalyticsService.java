package com.example.anusha.job_trail.analytics;

import com.example.anusha.job_trail.analytics.dto.ConversionResponse;
import com.example.anusha.job_trail.analytics.dto.FunnelResponse;
import com.example.anusha.job_trail.analytics.dto.FunnelStageCount;
import com.example.anusha.job_trail.analytics.dto.SourceResponseRate;
import com.example.anusha.job_trail.analytics.dto.StageConversion;
import com.example.anusha.job_trail.analytics.dto.StageDuration;
import com.example.anusha.job_trail.analytics.dto.TimeInStageResponse;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistory;
import com.example.anusha.job_trail.status.StatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Read-only queries over {@code status_history}, scoped to the caller's own
 * applications. Every method fetches the same flat, application-ordered row
 * list from {@link StatusHistoryRepository} and folds it in memory — the
 * table is small per user and this keeps the funnel/conversion/time-in-stage
 * math in one place, in plain Java, instead of three bespoke aggregate
 * queries that would each need re-deriving if the pipeline ever changes.
 */
@Service
public class AnalyticsService {

    // The forward pipeline a funnel/conversion chart walks. REJECTED and
    // GHOSTED are exits, not steps on the way to an offer, so they're
    // deliberately excluded here — see RESPONSE_STAGES for where they do
    // count.
    private static final List<Stage> FUNNEL_STAGES = List.of(
            Stage.SAVED, Stage.APPLIED, Stage.SCREEN, Stage.INTERVIEW, Stage.FINAL, Stage.OFFER);

    // A stage counts as "the employer responded" once it's anything past
    // APPLIED that isn't silence. REJECTED is a response (a negative one);
    // GHOSTED is specifically the absence of one, so it's left out.
    private static final Set<Stage> RESPONSE_STAGES =
            EnumSet.of(Stage.SCREEN, Stage.INTERVIEW, Stage.FINAL, Stage.OFFER, Stage.REJECTED);

    private static final String UNKNOWN_SOURCE = "Unknown";

    private final StatusHistoryRepository statusHistoryRepository;

    public AnalyticsService(StatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional(readOnly = true)
    public FunnelResponse funnel(UUID userId) {
        Map<UUID, Set<Stage>> stagesReachedByApplication =
                groupStagesReachedByApplication(statusHistoryRepository.findAllForUserOrderedByApplicationAndTime(userId));

        List<FunnelStageCount> stages = FUNNEL_STAGES.stream()
                .map(stage -> new FunnelStageCount(stage, countReached(stagesReachedByApplication, stage)))
                .toList();

        return new FunnelResponse(stagesReachedByApplication.size(), stages);
    }

    @Transactional(readOnly = true)
    public ConversionResponse conversion(UUID userId) {
        List<StatusHistory> history = statusHistoryRepository.findAllForUserOrderedByApplicationAndTime(userId);
        Map<UUID, Set<Stage>> stagesReachedByApplication = groupStagesReachedByApplication(history);

        List<StageConversion> stageConversions = new ArrayList<>();
        for (int i = 0; i < FUNNEL_STAGES.size() - 1; i++) {
            Stage from = FUNNEL_STAGES.get(i);
            Stage to = FUNNEL_STAGES.get(i + 1);
            long fromCount = countReached(stagesReachedByApplication, from);
            long toCount = countReached(stagesReachedByApplication, to);
            stageConversions.add(new StageConversion(from, to, fromCount, toCount, rate(toCount, fromCount)));
        }

        return new ConversionResponse(stageConversions, responseRateBySource(history, stagesReachedByApplication));
    }

    @Transactional(readOnly = true)
    public TimeInStageResponse timeInStage(UUID userId) {
        List<StatusHistory> history = statusHistoryRepository.findAllForUserOrderedByApplicationAndTime(userId);

        // Days spent in a stage, keyed by the stage being departed, one
        // sample per completed (row -> next row) transition. The row list
        // is already ordered application-then-time, so a stage's interval
        // only closes when the next row belongs to the same application.
        Map<Stage, List<Double>> daysByStage = new EnumMap<>(Stage.class);
        StatusHistory previous = null;
        for (StatusHistory row : history) {
            if (previous != null && sameApplication(previous, row)) {
                double days = Duration.between(previous.getCreatedAt(), row.getCreatedAt()).getSeconds() / 86400.0;
                daysByStage.computeIfAbsent(previous.getStage(), stage -> new ArrayList<>()).add(days);
            }
            previous = row;
        }

        List<StageDuration> stages = daysByStage.entrySet().stream()
                .map(entry -> new StageDuration(entry.getKey(), average(entry.getValue()), entry.getValue().size()))
                .sorted(Comparator.comparing(StageDuration::stage))
                .toList();

        return new TimeInStageResponse(stages);
    }

    private List<SourceResponseRate> responseRateBySource(List<StatusHistory> history,
                                                            Map<UUID, Set<Stage>> stagesReachedByApplication) {
        Map<UUID, String> sourceByApplication = new LinkedHashMap<>();
        for (StatusHistory row : history) {
            sourceByApplication.putIfAbsent(row.getApplication().getId(), normalizeSource(row.getApplication().getSource()));
        }

        // [total, responded] per source, in a TreeMap so the response is
        // stable and alphabetical rather than in first-seen order.
        Map<String, long[]> countsBySource = new TreeMap<>();
        for (Map.Entry<UUID, Set<Stage>> entry : stagesReachedByApplication.entrySet()) {
            long[] counts = countsBySource.computeIfAbsent(sourceByApplication.get(entry.getKey()), source -> new long[2]);
            counts[0]++;
            if (entry.getValue().stream().anyMatch(RESPONSE_STAGES::contains)) {
                counts[1]++;
            }
        }

        return countsBySource.entrySet().stream()
                .map(entry -> new SourceResponseRate(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        rate(entry.getValue()[1], entry.getValue()[0])))
                .toList();
    }

    private static Map<UUID, Set<Stage>> groupStagesReachedByApplication(List<StatusHistory> history) {
        Map<UUID, Set<Stage>> stagesReachedByApplication = new LinkedHashMap<>();
        for (StatusHistory row : history) {
            stagesReachedByApplication
                    .computeIfAbsent(row.getApplication().getId(), id -> EnumSet.noneOf(Stage.class))
                    .add(row.getStage());
        }
        return stagesReachedByApplication;
    }

    private static long countReached(Map<UUID, Set<Stage>> stagesReachedByApplication, Stage stage) {
        return stagesReachedByApplication.values().stream().filter(reached -> reached.contains(stage)).count();
    }

    private static boolean sameApplication(StatusHistory a, StatusHistory b) {
        return a.getApplication().getId().equals(b.getApplication().getId());
    }

    private static String normalizeSource(String source) {
        return (source == null || source.isBlank()) ? UNKNOWN_SOURCE : source;
    }

    private static double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
