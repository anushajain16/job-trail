package com.example.anusha.job_trail.status;

import com.example.anusha.job_trail.application.Application;
import com.example.anusha.job_trail.status.dto.StatusHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns the status_history table. This is the only place a row is ever
 * written — there's no update or delete path, by design (see
 * {@link StatusHistory}). Ownership of the underlying application (is this
 * caller's?) is the caller's job; this service only knows about
 * applications that already resolved to a real, owned entity.
 */
@Service
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;

    public StatusHistoryService(StatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    /**
     * Writes the very first history row for a newly created application,
     * at whatever stage it starts in. Distinct from {@link #recordTransition}
     * because there's no prior stage to compare against — this is the one
     * call site allowed to write a row that "transitions" to the stage the
     * application is already sitting at.
     */
    @Transactional
    public void recordInitial(Application application) {
        statusHistoryRepository.save(new StatusHistory(application, application.getCurrentStage()));
    }

    /**
     * Appends a history row for the transition and moves the application's
     * {@code currentStage} cache in lockstep, in the same transaction —
     * the two can never observably drift apart. A "transition" to the
     * stage the application is already in isn't one, so it's rejected
     * rather than silently logged as a duplicate event.
     */
    @Transactional
    public void recordTransition(Application application, Stage newStage) {
        if (application.getCurrentStage() == newStage) {
            throw new IllegalArgumentException("Application is already in stage " + newStage);
        }
        statusHistoryRepository.save(new StatusHistory(application, newStage));
        application.setCurrentStage(newStage);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistory(UUID applicationId) {
        return statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(entry -> new StatusHistoryResponse(entry.getId(), entry.getStage(), entry.getCreatedAt()))
                .toList();
    }
}
