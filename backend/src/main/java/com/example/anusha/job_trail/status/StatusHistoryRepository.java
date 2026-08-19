package com.example.anusha.job_trail.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

    // Backs both the timeline endpoint and analytics: always read in
    // chronological order, always scoped to one application. Named by the
    // Java property (createdAt, inherited from BaseEntity) even though the
    // column it maps to is renamed to changed_at — Spring Data derives
    // queries from bean properties, not column names.
    List<StatusHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
