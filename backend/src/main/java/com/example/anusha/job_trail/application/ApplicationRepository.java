package com.example.anusha.job_trail.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // Every read/write in this feature is scoped to the caller's own rows —
    // both methods below exist so a handler can never accidentally load (or
    // mutate) another user's application by id alone.
    Page<Application> findByUserId(UUID userId, Pageable pageable);

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);
}
