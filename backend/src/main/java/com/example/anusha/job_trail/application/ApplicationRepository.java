package com.example.anusha.job_trail.application;

import com.example.anusha.job_trail.status.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // Every read/write in this feature is scoped to the caller's own rows —
    // both methods below exist so a handler can never accidentally load (or
    // mutate) another user's application by id alone.
    Page<Application> findByUserId(UUID userId, Pageable pageable);

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    //Finds potential ghosted applications: must not be in an excluded/terminal stage, must have a deadline, the deadline must have passed, and the application must not have been updated recently
    @Query("SELECT a FROM Application a WHERE a.currentStage NOT IN :excludedStages "
            + "AND a.deadline IS NOT NULL AND a.deadline < :today AND a.updatedAt < :staleBefore")
    List<Application> findGhostCandidates(@Param("excludedStages") Collection<Stage> excludedStages,
                                           @Param("today") LocalDate today,
                                           @Param("staleBefore") Instant staleBefore);

    // Finds non-terminal applications whose deadline falls between today and a future lookahead date. These applications can then be used by a reminder job.
    // User is join-fetched: ReminderSender reads application.getUser().getEmail()
    // asynchronously, after this method's read-only transaction has already
    // closed, so the association must be initialized here — a lazy proxy
    // touched outside its owning session/transaction throws instead of loading.
    @Query("SELECT a FROM Application a JOIN FETCH a.user WHERE a.currentStage NOT IN :excludedStages "
            + "AND a.deadline IS NOT NULL AND a.deadline BETWEEN :today AND :lookaheadUntil")
    List<Application> findUpcomingDeadlines(@Param("excludedStages") Collection<Stage> excludedStages,
                                             @Param("today") LocalDate today,
                                             @Param("lookaheadUntil") LocalDate lookaheadUntil);
}
