package com.example.anusha.job_trail.googlecalendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleConnectionRepository extends JpaRepository<GoogleConnection, UUID> {

    Optional<GoogleConnection> findByUserId(UUID userId);

    // Disconnecting deletes the row outright rather than clearing its
    // fields — there's nothing worth keeping once the credential it
    // exists to hold is gone, and a plain existence check is then enough
    // to answer "is this user connected?".
    void deleteByUserId(UUID userId);
}
