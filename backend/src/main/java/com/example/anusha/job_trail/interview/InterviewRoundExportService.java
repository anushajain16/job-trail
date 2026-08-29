package com.example.anusha.job_trail.interview;

import com.example.anusha.job_trail.common.csv.CsvExport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Flattens the caller's interview rounds into a CSV export — every round
 * across every application, application and company/role included since a
 * round on its own doesn't say which application it belongs to.
 */
@Service
public class InterviewRoundExportService {

    private static final List<String> HEADER = List.of(
            "Company", "Role", "Round Type", "Scheduled At", "Interviewer", "Questions Asked", "Notes", "Reflection");

    private final InterviewRoundRepository interviewRoundRepository;

    public InterviewRoundExportService(InterviewRoundRepository interviewRoundRepository) {
        this.interviewRoundRepository = interviewRoundRepository;
    }

    @Transactional(readOnly = true)
    public void export(UUID userId, HttpServletResponse response) {
        List<List<String>> rows = new ArrayList<>();
        for (InterviewRound round : interviewRoundRepository.findAllForUserOrderedByApplicationAndSchedule(userId)) {
            rows.add(List.of(
                    round.getApplication().getCompany(),
                    round.getApplication().getRole(),
                    round.getRoundType(),
                    round.getScheduledAt() != null ? DateTimeFormatter.ISO_INSTANT.format(round.getScheduledAt()) : "",
                    nullToEmpty(round.getInterviewerName()),
                    nullToEmpty(round.getQuestionsAsked()),
                    nullToEmpty(round.getNotes()),
                    nullToEmpty(round.getReflection())
            ));
        }

        CsvExport.write(response, "jobtrail-interview-rounds-export.csv", HEADER, rows);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
