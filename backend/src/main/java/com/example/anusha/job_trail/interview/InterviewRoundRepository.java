package com.example.anusha.job_trail.interview;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
