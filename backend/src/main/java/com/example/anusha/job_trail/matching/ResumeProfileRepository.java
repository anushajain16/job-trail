package com.example.anusha.job_trail.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, UUID> {

    Optional<ResumeProfile> findByUserId(UUID userId);
}
