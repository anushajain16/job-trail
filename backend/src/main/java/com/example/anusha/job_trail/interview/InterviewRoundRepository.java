package com.example.anusha.job_trail.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    // Chronological listing for one application — the detail view's rounds
    // tab reads this directly.
    List<InterviewRound> findByApplicationIdOrderByScheduledAtAsc(UUID applicationId);

    // The auth boundary for the flat PATCH/DELETE /api/interviews/{id}
    // endpoints, which don't carry an application id in the path: a round
    // whose application isn't owned by this caller doesn't exist as far as
    // this query is concerned, same "404, not 403" shape as
    // ApplicationRepository.findByIdAndUserId.
    Optional<InterviewRound> findByIdAndApplicationUserId(UUID id, UUID userId);

    // The CSV export's one query: every round across every application the
    // caller owns, grouped by application then chronological within it.
    // The application is join-fetched — the export reads its company/role
    // on every row, and it's LAZY by default, so this avoids an N+1 as the
    // result set is walked.
    @Query("SELECT ir FROM InterviewRound ir JOIN FETCH ir.application a "
            + "WHERE a.user.id = :userId ORDER BY a.id ASC, ir.scheduledAt ASC")
    List<InterviewRound> findAllForUserOrderedByApplicationAndSchedule(@Param("userId") UUID userId);
}
