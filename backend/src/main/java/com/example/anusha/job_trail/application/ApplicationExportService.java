package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.common.csv.CsvExport;
import com.example.anusha.job_trail.status.Stage;
import com.example.anusha.job_trail.status.StatusHistory;
import com.example.anusha.job_trail.status.StatusHistoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flattens the caller's applications into the CSV export — the paginated
 * list's fields, plus the one thing not stored on {@link Application}
 * itself: "applied date". There's no such column; it's read off
 * {@code status_history} as the first time each application reached
 * {@link Stage#APPLIED}, the same "fold the event log in memory" approach
 * {@code AnalyticsService} uses, and left blank for an application that
 * never has (still SAVED, or applied outside this tool's tracking).
 */
@Service
public class ApplicationExportService {

    private static final List<String> HEADER = List.of(
            "Company", "Role", "Location", "Salary Range", "Source", "Current Stage", "Applied Date", "Deadline", "Notes");

    private final ApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public ApplicationExportService(ApplicationRepository applicationRepository, StatusHistoryRepository statusHistoryRepository) {
        this.applicationRepository = applicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional(readOnly = true)
    public void export(UUID userId, HttpServletResponse response) {
        List<Application> applications = applicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, LocalDate> appliedDateByApplicationId = firstAppliedDateByApplication(userId);

        List<List<String>> rows = new ArrayList<>();
        for (Application application : applications) {
            rows.add(List.of(
                    nullToEmpty(application.getCompany()),
                    nullToEmpty(application.getRole()),
                    nullToEmpty(application.getLocation()),
                    salaryRange(application.getSalaryMin(), application.getSalaryMax()),
                    nullToEmpty(application.getSource()),
                    application.getCurrentStage().name(),
                    nullToEmpty(appliedDateByApplicationId.get(application.getId())),
                    nullToEmpty(application.getDeadline()),
                    nullToEmpty(application.getNotes())
            ));
        }

        CsvExport.write(response, "jobtrail-export.csv", HEADER, rows);
    }

    private Map<UUID, LocalDate> firstAppliedDateByApplication(UUID userId) {
        Map<UUID, LocalDate> firstApplied = new HashMap<>();
        // Rows arrive ordered application-then-time, so the first APPLIED
        // row seen for an application id is its earliest one — no sorting
        // or comparison needed on top of the query's own ordering.
        for (StatusHistory entry : statusHistoryRepository.findAllForUserOrderedByApplicationAndTime(userId)) {
            if (entry.getStage() == Stage.APPLIED) {
                firstApplied.putIfAbsent(entry.getApplication().getId(), entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate());
            }
        }
        return firstApplied;
    }

    private static String salaryRange(Integer min, Integer max) {
        if (min == null && max == null) {
            return "";
        }
        if (min != null && max != null) {
            return min + "-" + max;
        }
        return String.valueOf(min != null ? min : max);
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
