package com.example.anusha.job_trail.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    // Backs both the timeline endpoint and analytics: always read in
    // chronological order, always scoped to one application. Named by the
    // Java property (createdAt, inherited from BaseEntity) even though the
    // column it maps to is renamed to changed_at — Spring Data derives
    // queries from bean properties, not column names.
    List<StatusHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    // Backs the analytics feature: every row for every application the user
    // owns, ordered so a single pass (grouped by application, chronological
    // within it) is enough to derive funnel/conversion/time-in-stage without
    // a second query per application. The application is fetched eagerly —
    // analytics needs its id and source on every row, and it's LAZY by
    // default — so this doesn't turn into an N+1 as the result set is walked.
    @Query("SELECT sh FROM StatusHistory sh JOIN FETCH sh.application a "
            + "WHERE a.user.id = :userId ORDER BY a.id ASC, sh.createdAt ASC")
    List<StatusHistory> findAllForUserOrderedByApplicationAndTime(@Param("userId") UUID userId);
}
